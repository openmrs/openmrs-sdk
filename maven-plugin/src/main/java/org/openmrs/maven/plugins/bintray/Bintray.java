package org.openmrs.maven.plugins.bintray;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Bintray {
    private String username;
    private String password;

    public Bintray() {}

    public Bintray(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setCredentials(String username, String password){
        this.username = username;
        this.password = password;
    }

    public List<BintrayId> getAvailablePackages(String owner, String repo){
        String url = String.format("https://api.bintray.com/repos/%s/%s/packages", owner, repo);
        GetMethod get = new GetMethod(url);
        try {
            new HttpClient().executeMethod(get);
            if (get.getStatusLine() == null || get.getStatusCode() != 200) {
                throw new IOException(get.getStatusLine().toString());
            }
            ObjectMapper mapper = new ObjectMapper();
            CollectionType idListType = mapper.getTypeFactory().constructCollectionType(List.class, BintrayId.class);
            List<BintrayId> owaIds = mapper.readValue(get.getResponseBodyAsStream(), idListType);
            return owaIds;
        } catch (IOException e) {
            //avoid NPE
            if(get.getStatusLine() != null && get.getStatusCode() == 404){
                throw new RuntimeException("Bintray repository not found!", e);
            } else throw new RuntimeException(e);
        }
    }

    public BintrayPackage getPackageMetadata(String owner, String repo, String name){
        String url = String.format("https://api.bintray.com/packages/%s/%s/%s", owner, repo, name);
        GetMethod get = new GetMethod(url);
        try {
            new HttpClient().executeMethod(get);
            if (get.getStatusLine() == null || get.getStatusCode() != 200) {
                throw new IOException(get.getStatusLine().toString());
            }
            JsonParser parser =  new JsonFactory().createParser(get.getResponseBodyAsStream());
            parser.setCodec(new ObjectMapper());
            return parser.readValueAs(BintrayPackage.class);
        } catch (IOException e) {
            if(get.getStatusLine() != null &&  get.getStatusCode() == 404){
                return null;
            } else throw new RuntimeException(e);
        }
    }

    public List<BintrayFile> getPackageFiles(String owner, String repo, String name, String version){
        String url = String.format("https://api.bintray.com/packages/%s/%s/%s/versions/%s/files", owner, repo, name, version);
        GetMethod get = new GetMethod(url);
        try {
            new HttpClient().executeMethod(get);
            if (get.getStatusLine() == null || get.getStatusCode() != 200) {
                throw new IOException(get.getStatusLine().toString());
            }
            ObjectMapper mapper = new ObjectMapper();
            CollectionType fileListType = mapper.getTypeFactory().constructCollectionType(List.class, BintrayFile.class);
            List<BintrayFile> bintrayFiles = mapper.readValue(get.getResponseBodyAsStream(), fileListType);
            return bintrayFiles;
        } catch (IOException e) {
            if(get.getStatusLine() != null &&  get.getStatusCode() == 404){
                throw new RuntimeException("Bintray package not found!", e);
            } else throw new RuntimeException(e);
        }
    }

    public void downloadPackage(File destDirectory, String owner, String repo, String name, String version){
        List<BintrayFile> files = getPackageFiles(owner, repo, name, version);
        for(BintrayFile file : files){
            downloadFile(file, destDirectory, null);
        }
    }

    /**
     * @param file
     * @param destDirectory
     * @param filename optional, if not passed, name is bintray file name
     * @return
     */
    public File downloadFile(BintrayFile file, File destDirectory, String filename){
        try {
            if(filename == null){
                filename = file.getName();
            }
            File destFile = new File(destDirectory, filename);
            if(destFile.exists()){
                throw new RuntimeException("Cannot overwrite file: "+file.getPath());
            }
            String url = String.format("https://dl.bintray.com/%s/%s/%s", file.getOwner(), file.getRepository(), file.getPath());
            URL fileUrl = new URL(url);
            FileUtils.copyURLToFile(fileUrl, destFile, 2000, 5000);
            return destFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadPackage(File destDirectory, BintrayPackage bintrayPackage){
        downloadPackage(
                destDirectory,
                bintrayPackage,
                bintrayPackage.getLatestVersion());
    }
    public void downloadPackage(File destDirectory, BintrayPackage bintrayPackage, String version){
        downloadPackage(
                destDirectory,
                bintrayPackage.getOwner(),
                bintrayPackage.getRepository(),
                bintrayPackage.getName(),
                version);
    }

    public BintrayPackage createPackage(String owner, String repository, CreatePackageRequest request){
        String url = String.format("https://api.bintray.com/packages/%s/%s/", owner, repository);
        PostMethod post = new PostMethod(url);
        assertAuthorized();
        post.addRequestHeader("Authorization", "Basic "+ new String(Base64.encodeBase64((username+":"+password).getBytes())));
        ObjectMapper mapper = new ObjectMapper();
        try{
            post.setRequestEntity(new ByteArrayRequestEntity(mapper.writeValueAsBytes(request)));
            new HttpClient().executeMethod(post);
            if(post.getStatusLine() == null || post.getStatusCode()==401){
                throw new IOException("Unauthorized, this user have no rights to publish packages as "+owner+", or API key is invalid");
            }
            JsonParser parser =  new JsonFactory().createParser(post.getResponseBodyAsStream());
            parser.setCodec(new ObjectMapper());
            BintrayPackage bintrayPackage = parser.readValueAs(BintrayPackage.class);
            //there is response body so mapper will return empty object, not null
            if(bintrayPackage.getName() != null){
                return bintrayPackage;
            } else {
                throw new RuntimeException("Failed to create package, cause : "+(post.getStatusCode()+" - "+ post.getStatusText()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create package", e);
        }
    }

    private void assertAuthorized() {
        if(username==null||password==null){
            throw new IllegalStateException("Cannot create package without credentials");
        }
    }

    public void uploadFile(BintrayPackage bintrayPackage, String targetPath, String version, File file) {
        uploadFile(bintrayPackage, targetPath, file, version, "application/zip");
    }

    public void uploadFile(BintrayPackage bintrayPackage, String targetPath, File file, String version, String contentType){
        String url = String.format("https://api.bintray.com/content/%s/%s/%s/%s/%s?publish=1",
                bintrayPackage.getOwner(),
                bintrayPackage.getRepository(),
                bintrayPackage.getName(),
                version,
                targetPath);
        PutMethod put = new PutMethod(url);
        assertAuthorized();
        put.addRequestHeader("Authorization", "Basic "+ new String(Base64.encodeBase64((username+":"+password).getBytes())));
        put.setRequestEntity(new FileRequestEntity(file, contentType));

        try{
            new HttpClient().executeMethod(put);
            validateAuthorizedRequestResult(bintrayPackage.getOwner(), put, 201);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload files to package: "+bintrayPackage.getName(), e);
        }
    }

    private void validateAuthorizedRequestResult(String authorizedUser, EntityEnclosingMethod method, int expectedStatusCode) throws Exception {
        if(method.getStatusLine() == null){
            throw new Exception("Failed to send request, status line is not available");
        } else if(method.getStatusCode()==401){
            throw new IOException("Unauthorized, this user have no rights to publish packages as "+authorizedUser+", or API key is invalid");
        } else if(method.getStatusCode() != expectedStatusCode){
            throw new Exception(method.getStatusCode()+" : "+ method.getStatusText());
        }
    }

    public void createVersion(String owner, String repository, String packageName, String versionName){
        String url = String.format("https://api.bintray.com/packages/%s/%s/%s/versions", owner, repository, packageName);
        PostMethod post = new PostMethod(url);
        assertAuthorized();
        post.addRequestHeader("Authorization", "Basic "+ new String(Base64.encodeBase64((username+":"+password).getBytes())));
        post.setRequestEntity(new ByteArrayRequestEntity(("{\"name\":\""+versionName+"\"}").getBytes(), "application/json"));
        try{
            new HttpClient().executeMethod(post);
            validateAuthorizedRequestResult(owner, post, 201);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create version in package: "+packageName, e);
        }
    }

    public void publishVersion(String owner, String repository, String packageName, String versionName){
        String url = String.format("https://api.bintray.com/content/%s/%s/%s/%s/publish", owner, repository, packageName, versionName);
        PostMethod post = new PostMethod(url);
        assertAuthorized();
        post.addRequestHeader("Authorization", "Basic "+ new String(Base64.encodeBase64((username+":"+password).getBytes())));
        post.setRequestEntity(new ByteArrayRequestEntity(("{\"publish-wait-for-secs\":\"-1\"}").getBytes(), "application/json"));
        try{
            new HttpClient().executeMethod(post);
            validateAuthorizedRequestResult(owner, post, 200);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish version "+versionName+" in package: "+packageName, e);
        }
    }
}

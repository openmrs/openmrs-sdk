package org.openmrs.maven.plugins.model;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

/**
 *
 */
public class SdkStatistics {

    private static final String SDK_STATS_FILE_NAME = "sdk-stats.properties";
    private static final String DATE_FORMAT = "dd-M-yyyy";

    private Properties statistics = new Properties();

    public SdkStatistics createSdkStatsFile(boolean agree) throws MojoExecutionException {
        loadStatsFromResource();
        initData();
        setStatsEnabled(agree);
        save();
        return new SdkStatistics(statistics);
    }

    public static SdkStatistics loadStatistics() throws IllegalStateException, MojoExecutionException {
        return new SdkStatistics(new File(Server.getServersPath(), SDK_STATS_FILE_NAME));
    }

    public SdkStatistics(){}

    private SdkStatistics(Properties statistics) throws MojoExecutionException {
        this.statistics = statistics;
    }

    SdkStatistics(File file) throws IllegalStateException, MojoExecutionException {
        if (file.exists()) {
            loadStatsFromFile(file);
        } else {
            throw new IllegalStateException("File does not exist");
        }
        setSdkVersion(SDKConstants.getSDKInfo().getVersion());
    }

    /**
     * Checks if there is more then 7 days since last sendReport. If yes, send statistics
     * @throws MojoExecutionException
     */
    public void sendReport(Wizard wizard) throws MojoExecutionException {
        if( checkIfOneWeekFromLastReport() && getStatsEnabled()){
            setLastReported();
            save();
            try {
                postToGoogleForm();
            } catch (IOException e) {
                wizard.showMessage("Anonymous usage statistics could not be sent due to " + e.getMessage());
            }
        }
    }

    /**
     * Checks if one week has passed since last report
     *
     * @return
     */
    private boolean checkIfOneWeekFromLastReport(){
        Date lastReport = getLastReported();
        Date currentDate = new Date();

        if (lastReport != null) {
            return  (currentDate.getTime() - lastReport.getTime()) / (60 * 60 * 1000 * 24) % 60 > 7;
        } else {
            return true;
        }
    }

    /**
     * Send statistics to Google form
     * @throws IOException
     */
    private void postToGoogleForm() throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        String uri = buildUrl();
        HttpPost httpPost = new HttpPost(uri);

        HttpResponse response = httpClient.execute(httpPost);
    }

    /**
     * Generate URL to post data
     * @return
     */
    private String buildUrl() {
        StringBuilder builder = new StringBuilder();
        builder.append("https://docs.google.com/forms/d/e/1FAIpQLSd1OYp9wiAp0YC09kHlrwnoEGYwmjYY9hz2Mh_D8tUT8yIgmw/formResponse?");
        builder.append("entry.810765356="+statistics.getProperty("statsSetupCalls"));
        builder.append("&entry.241459878="+statistics.getProperty("statsDeployCalls"));
        builder.append("&entry.2099549842="+statistics.getProperty("statsPullCalls"));
        builder.append("&entry.1422428008="+statistics.getProperty("statsBuildCalls"));
        builder.append("&entry.774969394="+statistics.getProperty("statsWatchCalls"));
        builder.append("&entry.308937646="+statistics.getProperty("statsHelpCalls"));
        builder.append("&entry.1768434209="+statistics.getProperty("statsUndeployCalls"));
        builder.append("&entry.2073436151="+statistics.getProperty("statsUnwatchCalls"));
        builder.append("&entry.259090049="+statistics.getProperty("statsLastReported"));
        builder.append("&entry.1184373549="+statistics.getProperty("statsResetCalls"));
        builder.append("&entry.2023918399="+statistics.getProperty("statsLastUsed"));
        builder.append("&entry.138430639="+statistics.getProperty("statsInfoCalls"));
        builder.append("&entry.783545123="+statistics.getProperty("statsDeleteCalls"));
        builder.append("&entry.399480215="+statistics.getProperty("statsSdkVersion"));
        builder.append("&entry.677412410="+statistics.getProperty("statsUser"));
        builder.append("&entry.678099034="+statistics.getProperty("statsCreate-projectCalls"));
        builder.append("&entry.1559628016="+statistics.getProperty("statsRunCalls"));
        builder.append("&entry.2088950996="+statistics.getProperty("statsPrCalls"));
        builder.append("&entry.1875021849="+statistics.getProperty("statsCloneCalls"));
        builder.append("&entry.2038776811="+statistics.getProperty("statsReleaseCalls"));
        builder.append("&entry.1366642496="+statistics.getProperty("statsBuild-distroCalls"));

        return builder.toString();
    }

    /**
     * Init data when creating sdk-stats.properties file
     */
    private void initData() {
        setUser();
        setLastUsed();
        setSdkVersion(SDKConstants.getSDKInfo().getVersion());
    }

    /**
     * Increment usage of specific goal
     * @param goal
     */
    public void incrementGoal(String goal){
        String key = String.format("stats%sCalls", goal);
        String property = statistics.getProperty(key);
        int calls ;
        if(StringUtils.isBlank(property)){
            calls = 0;
        } else {
            calls = Integer.parseInt(property);
        }
        calls++;
        statistics.setProperty(key, String.valueOf(calls));
    }

    /**
     * Set current SDK version
     * @param version
     */
    private void setSdkVersion(String version) {
        statistics.setProperty("statsSdkVersion", version);
    }

    /**
     * Set statsEnabled property, true if user agrees to gather data
     * @param statsEnabled
     */
    public void setStatsEnabled(boolean statsEnabled) {
        statistics.setProperty("statsEnabled", String.valueOf(statsEnabled));
    }

    /**
     * Set current date as lastUsed property
     */
    public void setLastUsed() {
        statistics.setProperty("statsLastUsed", getDate());
    }

    /**
     * Generate random user UUID and set it
     */
    private void setUser() {
        String user = UUID.randomUUID().toString();
        statistics.setProperty("statsUser", user);
    }

    /**
     * Set current date as lastReported
     */
    private void setLastReported(){
        statistics.setProperty("statsLastReported", getDate());
    }

    /**
     * Get user name
     * @return
     */
    private String getUser() {
        return statistics.getProperty("statsUser");
    }

    /**
     * Get statsEnabled property
     * @return
     */
    public boolean getStatsEnabled(){
        return Boolean.parseBoolean(statistics.getProperty("statsEnabled"));
    }

    /**
     * Get last date  that statistics were reported
     * @return
     */
    private Date getLastReported(){
        String statsLastReported = statistics.getProperty("statsLastReported");
        if(StringUtils.isBlank(statsLastReported)){
            return null;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        Date date;
        try {
            date = dateFormat.parse(statsLastReported);
            return date;
        } catch (ParseException e) {
            throw new RuntimeException("Couldn't parse String to Date", e);
        }
    }

    public int getGoalCalls(String goal) {
        return Integer.parseInt(statistics.getProperty("stats"+goal+"Calls"));
    }

    /**
     * Get last date that sdk was used
     * @return
     */
    private Date getLastUsed(){
        String statsLastUsed = statistics.getProperty("statsLastUsed");
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        Date date;
        try {
            date = dateFormat.parse(statsLastUsed);
            return date;
        } catch (ParseException e) {
            throw new RuntimeException("Couldn't parse String to Date", e);
        }
    }

    /**
     * Get current date as String
     * @return String
     */
    private String getDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String date = dateFormat.format(new Date());
        return date;
    }

    /**
     * Loads statistics from file
     * @param file
     * @throws MojoExecutionException
     */
    private void loadStatsFromFile(File file) throws MojoExecutionException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            statistics.load(in);
            in.close();
        }
        catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Loads initial statistics from resource
     * @throws MojoExecutionException
     */
    public void loadStatsFromResource() throws MojoExecutionException {
        InputStream in = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream(SDK_STATS_FILE_NAME);
            statistics.load(in);
            in.close();
        }
        catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Save statistics to default directory
     * @throws MojoExecutionException
     */
    public void save() throws MojoExecutionException {
        new File(Server.getServersPath()).mkdirs();
        saveTo(new File(Server.getServersPath(), SDK_STATS_FILE_NAME));
    }

    /**
     * Save statistics to specific dir
     * @param path
     * @throws MojoExecutionException
     */
    public void saveTo(File path) throws MojoExecutionException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            statistics.store(out, null);
            out.close();
        }
        catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(out);
        }
    }
}

package org.openmrs.maven.plugins.model;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.UUID;

import static org.openmrs.maven.plugins.utility.PropertiesUtils.loadPropertiesFromFile;
import static org.openmrs.maven.plugins.utility.PropertiesUtils.loadPropertiesFromResource;

/**
 *
 */
public class SdkStatistics {

    public static final String SDK_STATS_FILE_NAME = "sdk-stats.properties";

    private static final String DATE_FORMAT = "dd-M-yyyy";

    private Properties statistics = new Properties();

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public SdkStatistics createSdkStatsFile(boolean agree) throws MojoExecutionException {
        loadStatsFromResource(SDK_STATS_FILE_NAME);
        initData();
        setStatsEnabled(agree);
        save();
        return new SdkStatistics(statistics);
    }

    public static SdkStatistics loadStatistics() throws MojoExecutionException {
        return new SdkStatistics(Server.getServersPath().resolve(SDK_STATS_FILE_NAME).toFile());
    }

    public SdkStatistics(){}

    private SdkStatistics(Properties statistics) {
        this.statistics = statistics;
    }

    SdkStatistics(File file) throws MojoExecutionException {
        if (file.exists()) {
            loadStatsFromFile(file);
        } else {
            throw new MojoExecutionException("File does not exist");
        }
        setSdkVersion(SDKConstants.getSDKInfo().getVersion());
    }

    /**
     * Checks if there is more than 7 days since last sendReport. If yes, send statistics
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
        LocalDate lastReport = getLastReported();
        LocalDate currentDate = LocalDate.now();

        if (lastReport != null) {
            return ChronoUnit.DAYS.between(lastReport, currentDate) > 7;
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
        return "https://docs.google.com/forms/d/e/1FAIpQLSd1OYp9wiAp0YC09kHlrwnoEGYwmjYY9hz2Mh_D8tUT8yIgmw/formResponse?"
                + "entry.810765356=" + statistics.getProperty("statsSetupCalls")
                + "&entry.241459878=" + statistics.getProperty("statsDeployCalls")
                + "&entry.2099549842=" + statistics.getProperty("statsPullCalls")
                + "&entry.1422428008=" + statistics.getProperty("statsBuildCalls")
                + "&entry.774969394=" + statistics.getProperty("statsWatchCalls")
                + "&entry.308937646=" + statistics.getProperty("statsHelpCalls")
                + "&entry.1768434209=" + statistics.getProperty("statsUndeployCalls")
                + "&entry.2073436151=" + statistics.getProperty("statsUnwatchCalls")
                + "&entry.259090049=" + statistics.getProperty("statsLastReported")
                + "&entry.1184373549=" + statistics.getProperty("statsResetCalls")
                + "&entry.2023918399=" + statistics.getProperty("statsLastUsed")
                + "&entry.138430639=" + statistics.getProperty("statsInfoCalls")
                + "&entry.783545123=" + statistics.getProperty("statsDeleteCalls")
                + "&entry.399480215=" + statistics.getProperty("statsSdkVersion")
                + "&entry.677412410=" + statistics.getProperty("statsUser")
                + "&entry.678099034=" + statistics.getProperty("statsCreate-projectCalls")
                + "&entry.1559628016=" + statistics.getProperty("statsRunCalls")
                + "&entry.2088950996=" + statistics.getProperty("statsPrCalls")
                + "&entry.1875021849=" + statistics.getProperty("statsCloneCalls")
                + "&entry.2038776811=" + statistics.getProperty("statsReleaseCalls")
                + "&entry.1366642496=" + statistics.getProperty("statsBuild-distroCalls");
    }

    /**
     * Init data when creating sdk-stats.properties file
     */
    private void initData() throws MojoExecutionException {
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
    private LocalDate getLastReported() {
        String statsLastReported = statistics.getProperty("statsLastReported");
        if (StringUtils.isBlank(statsLastReported)){
            return null;
        }
        return LocalDate.parse(statsLastReported, dateTimeFormatter);
    }

    public int getGoalCalls(String goal) {
        return Integer.parseInt(statistics.getProperty("stats"+goal+"Calls"));
    }

    /**
     * Get last date that sdk was used
     * @return
     */
    private LocalDate getLastUsed() throws ParseException {
        String statsLastUsed = statistics.getProperty("statsLastUsed");
        return LocalDate.parse(statsLastUsed, dateTimeFormatter);
    }

    /**
     * Get current date as String
     * @return String
     */
    private String getDate() {
        return LocalDate.now().format(dateTimeFormatter);
    }

    /**
     * Loads statistics from a file
     *
     * @param file the file to load statistics from
     * @throws MojoExecutionException if an exception occurs loading or reading the file
     */
    private void loadStatsFromFile(File file) throws MojoExecutionException {
        loadPropertiesFromFile(file, statistics);
    }

    /**
     * Loads statistics from classpath resource
     *
     * @param resource the resource to load statistics from
     * @throws MojoExecutionException if an exception occurs loading or reading the resource
     */
    private void loadStatsFromResource(String resource) throws MojoExecutionException {
        loadPropertiesFromResource(resource, statistics);
    }

    /**
     * Save statistics to default directory
     * @throws MojoExecutionException
     */
    public void save() throws MojoExecutionException {
        Path serverPath = Server.getServersPath();
        File serverDir = serverPath.toFile();
        if (!serverDir.exists()) {
            serverDir.mkdirs();
        }

        saveTo(serverPath.resolve(SDK_STATS_FILE_NAME).toFile());
    }

    /**
     * Save statistics to specific file
     * @param file
     * @throws MojoExecutionException
     */
    public void saveTo(File file) throws MojoExecutionException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            statistics.store(out, null);
        }
        catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}

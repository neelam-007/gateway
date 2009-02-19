package com.l7tech.server.ems.ui;

import java.io.Serializable;

/**
 * This class object is used in the esm session to check if the EM license and SSL certificate are about to expire.
 */
public class CertLicExpiryStatus implements Serializable {
    private boolean isCertAboutToExpire;
    private boolean isLicAboutToExpire;
    private long daysCertAboutToExpire;
    private long daysLicAboutToExpire;
    private boolean warningDisplayed;

    public CertLicExpiryStatus() {
    }

    public long getDaysCertAboutToExpire() {
        return daysCertAboutToExpire;
    }

    public void setDaysCertAboutToExpire(long daysCertAboutToExpire) {
        this.daysCertAboutToExpire = daysCertAboutToExpire;
    }

    public long getDaysLicAboutToExpire() {
        return daysLicAboutToExpire;
    }

    public void setDaysLicAboutToExpire(long daysLicAboutToExpire) {
        this.daysLicAboutToExpire = daysLicAboutToExpire;
    }

    public boolean isCertAboutToExpire() {
        return isCertAboutToExpire;
    }

    public void setCertAboutToExpire(boolean certAboutToExpire) {
        isCertAboutToExpire = certAboutToExpire;
    }

    public boolean isLicAboutToExpire() {
        return isLicAboutToExpire;
    }

    public void setLicAboutToExpire(boolean licAboutToExpire) {
        isLicAboutToExpire = licAboutToExpire;
    }

    public boolean isWarningDisplayed() {
        return warningDisplayed;
    }

    public void setWarningDisplayed(boolean warningDisplayed) {
        this.warningDisplayed = warningDisplayed;
    }

    /**
     * If the license or ssl certificate is about to expire, what warning message will be displayed.
     * @param forLicense: a flag indicating if the warning message is for the license or the ssl certificate.
     * @return a string of the warning message.
     */
    public String getWarningMessage(boolean forLicense) {
        String name = forLicense? "The current Enterprise Manager License" : "The current SSL Certificate";
        long days = forLicense? getDaysLicAboutToExpire() : getDaysCertAboutToExpire();
        String daysStatment = days == 0? "less than one day" : days + (days == 1? "day" : " days");

        return name + " is about to expire in " + daysStatment + ".";
    }
}

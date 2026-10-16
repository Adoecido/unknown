package vegas.sony.account;

import vegas.sony.account.LicenseInfo;
import vegas.sony.account.LicenseInfoClient;
import vegas.sony.account.LicenseStatus;
import vegas.sony.account.LicenseStatusClient;

public class LicenseManager {
    private String licenseKey;
    private LicenseInfo licenseInfo;
    private static int[] opaqueState;

    public static int[] getOpaqueState() {
        return opaqueState;
    }

    public LicenseInfo getLicenseInfo() {
        return this.licenseInfo;
    }

    public static void setOpaqueState(int[] state) {
        opaqueState = state;
    }


    public LicenseStatus generateAccount() {
        if (this.licenseKey == null) {
            return null;
        }
        return new LicenseStatusClient().generateAccount(this.licenseKey);
    }

    public LicenseInfo fetchLicenseInfo(String licenseKey) {
        this.licenseInfo = new LicenseInfoClient().fetchLicenseInfo(licenseKey);
        if (this.licenseInfo != null) {
            this.licenseKey = licenseKey;
        }
        return this.licenseInfo;
    }

    static {
        if (LicenseManager.getOpaqueState() == null) {
            LicenseManager.setOpaqueState(new int[1]);
        }
    }
}


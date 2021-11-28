package md.leonis.crawler.moby;

import java.util.Date;

public final class Proxy {

    private final long id;
    private final String scheme;
    private final String host;
    private final Integer port;
    private final String userName;
    private final String password;
    private int peakWeight;
    private volatile ProxyStatus status;
    private volatile Long retryAfter;
    private final boolean isActive;

    public Proxy(long id, String scheme, String host, Integer port, String userName, String password) {
        this(id, scheme, host, port, userName, password, 0, ProxyStatus.NORMAL, null, true);
    }

    public Proxy(long id, String scheme, String host, Integer port, String userName, String password, int peakWeight, ProxyStatus status, Long retryAfter, boolean isActive) {

        this.id = id;
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.peakWeight = peakWeight;
        this.status = status;
        this.retryAfter = retryAfter;
        this.isActive = isActive;
    }

    public char[] getPasswordArray() {
        return password.toCharArray();
    }

    public String getUri() {
        return scheme + "://" + host;
    }

    public boolean isAvailable() {

        if (!isActive) {
            return false;
        }

        if (status == ProxyStatus.NORMAL) {
            return true;
        }

        if (null == retryAfter) {
            return true;
        } else {
            return new Date().getTime() > retryAfter;
        }
    }

    public long getId() {
        return id;
    }

    public String getScheme() {
        return scheme;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public int getPeakWeight() {
        return peakWeight;
    }

    public void setPeakWeight(int peakWeight) {
        this.peakWeight = peakWeight;
    }

    public int setAndReturnPeakWeight(int peakWeight) {
        this.peakWeight = peakWeight;
        return peakWeight;
    }

    public ProxyStatus getStatus() {
        return status;
    }

    public void setStatus(ProxyStatus status) {
        this.status = status;
    }

    public Long getRetryAfter() {
        return retryAfter;
    }

    public void setRetryAfter(Long retryAfter) {
        this.retryAfter = retryAfter;
    }

    public void setRetryAfterSec(Integer retryAfterSec) {

        if (retryAfterSec == null) {
            return;
        }

        if (retryAfterSec == 0) {
            retryAfterSec = 60;
        }
        this.retryAfter = new Date().getTime() + retryAfterSec * 1000L;
    }

    public boolean isActive() {
        return isActive;
    }

    public enum ProxyStatus {
        NORMAL, SPAMMED, BANNED, UNAVAILABLE
    }
}
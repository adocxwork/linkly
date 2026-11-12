package com.gupta.linkly.dto;

public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    /** Empty, or a UPI virtual payment address such as name@okbank. */
    public static final String UPI_ID_OR_EMPTY = "^$|^[a-zA-Z0-9._-]{2,256}@[a-zA-Z][a-zA-Z0-9]{1,63}$";

    /** Only web links: blocks javascript:, data:, file: and similar schemes. */
    public static final String HTTP_URL = "^(?i)https?://.+";

    /** Empty, or 3-30 URL-safe characters. */
    public static final String ALIAS_OR_EMPTY = "^$|^[a-zA-Z0-9_-]{3,30}$";
}

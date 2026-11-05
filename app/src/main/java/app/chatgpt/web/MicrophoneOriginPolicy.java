package app.chatgpt.web;

import java.net.URI;
import java.net.URISyntaxException;

final class MicrophoneOriginPolicy {
    static boolean isTrusted(String value) {
        if (value == null) return false;
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "chatgpt.com".equalsIgnoreCase(uri.getHost())
                    && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}

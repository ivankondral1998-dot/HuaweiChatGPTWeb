package app.chatgpt.web;

public final class MicrophoneOriginPolicyTest {
    public static void main(String[] args) {
        String[] allowed = {"https://chatgpt.com", "https://chatgpt.com/", "https://chatgpt.com:443/c/123", "https://CHATGPT.com/"};
        String[] denied = {null, "", "not a url", "http://chatgpt.com", "https://chatgpt.com.evil.example", "https://evil.example/chatgpt.com", "https://chatgpt.com@evil.example", "https://user@chatgpt.com", "https://chatgpt.com:444", "file:///chatgpt.com", "https://auth.openai.com", "https://sub.chatgpt.com", "https://chatgpt.com\\@evil.example"};
        for (String url : allowed) if (!MicrophoneOriginPolicy.isTrusted(url)) throw new AssertionError("Rejected trusted origin: " + url);
        for (String url : denied) if (MicrophoneOriginPolicy.isTrusted(url)) throw new AssertionError("Allowed untrusted origin: " + url);
        System.out.println("PASS: 17 microphone origin checks");
    }
}

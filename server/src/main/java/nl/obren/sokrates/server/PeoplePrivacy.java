package nl.obren.sokrates.server;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * What the server reveals about people ({@code serve -hidePeople}). When hidden, every contributor identity becomes a
 * pseudonym ({@code person-<10 hex>}, an HMAC of the lowercased email: the same person gets the same pseudonym in
 * every repository and call, and it cannot be turned back into the email without the key), user names are dropped,
 * email addresses inside free text (AI findings) are replaced by their pseudonym, and links to the HTML reports are
 * removed, because the reports themselves list names and emails (the server then does not serve them either).
 * The key is random per process unless given (SOKRATES_PSEUDONYM_KEY), so pseudonyms change on restart by default.
 */
public class PeoplePrivacy {
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+\\-\\[\\]]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");
    private static final PeoplePrivacy VISIBLE = new PeoplePrivacy(null);

    private final byte[] key;

    private PeoplePrivacy(byte[] key) {
        this.key = key;
    }

    public static PeoplePrivacy visible() {
        return VISIBLE;
    }

    /** Hidden people; {@code key} makes the pseudonyms stable across restarts (blank: a random key). */
    public static PeoplePrivacy hidden(String key) {
        if (StringUtils.isNotBlank(key)) {
            return new PeoplePrivacy(key.getBytes(StandardCharsets.UTF_8));
        }
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return new PeoplePrivacy(random);
    }

    public boolean isHidden() {
        return key != null;
    }

    /** The email itself, or its pseudonym. */
    public String person(String email) {
        if (!isHidden() || email == null) {
            return email;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] hash = mac.doFinal(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return "person-" + HexFormat.of().formatHex(hash).substring(0, 10);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String userName(String userName) {
        return isHidden() ? null : userName;
    }

    /** Free text with every email address replaced by its pseudonym. */
    public String text(String text) {
        if (!isHidden() || text == null) {
            return text;
        }
        return EMAIL.matcher(text).replaceAll(match -> person(match.group()));
    }

    /** A link to the HTML reports, or null: they are not served while people are hidden. */
    public String reportLink(String url) {
        return isHidden() ? null : url;
    }
}

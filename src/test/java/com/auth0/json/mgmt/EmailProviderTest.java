package com.auth0.json.mgmt;

import com.auth0.json.JsonTest;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class EmailProviderTest extends JsonTest<EmailProvider> {

    private static final String json = "{\"name\":\"provider\",\"enabled\":true,\"default_from_address\":\"https://google.com\",\"credentials\":{\"api_key\":\"key123\"},\"settings\":{}}";

    @Test
    public void shouldSerialize() throws Exception {
        EmailProvider provider = new EmailProvider("provider");
        provider.setEnabled(true);
        provider.setDefaultFromAddress("https://google.com");
        provider.setSettings(Collections.<String, Object>emptyMap());
        provider.setCredentials(new EmailProviderCredentials("key123"));

        String serialized = toJSON(provider);
        assertThat(serialized, is(notNullValue()));
        assertThat(serialized, is(equalTo(json)));
    }

    @Test
    public void shouldDeserialize() throws Exception {
        EmailProvider provider = fromJSON(json, EmailProvider.class);

        assertThat(provider, is(notNullValue()));
        assertThat(provider.getName(), is("provider"));
        assertThat(provider.getEnabled(), is(true));
        assertThat(provider.getDefaultFromAddress(), is("https://google.com"));
        assertThat(provider.getSettings(), is(notNullValue()));
        assertThat(provider.getCredentials(), is(notNullValue()));
    }

}
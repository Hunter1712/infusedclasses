package com.hunter1712.infusedclasses;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The settle type is a data contract: the bonus is priced off what landed, so vanilla's
 * difficulty scaling must not scale it a second time.
 */
class SettleTypeTest {
	@Test
	void settleNeverScalesWithDifficulty() {
		InputStream in = SettleTypeTest.class.getResourceAsStream(
				"/data/infusedclasses/damage_type/settle.json");
		assertNotNull(in);
		JsonObject json = JsonParser.parseReader(
				new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

		assertEquals("never", json.get("scaling").getAsString());
	}
}

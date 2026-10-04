package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierSinkTest {
	/** In-memory adapter: records every call so pure strip/apply order is assertable. */
	static final class RecordingSink implements ModifierSink {
		record Call(boolean add, Identifier attribute, Identifier modifier, double value,
				AttributeModifier.Operation operation) {
		}

		final List<Call> calls = new ArrayList<>();

		@Override
		public void removeModifier(Identifier attribute, Identifier modifier) {
			calls.add(new Call(false, attribute, modifier, 0, AttributeModifier.Operation.ADD_VALUE));
		}

		@Override
		public void addModifier(Identifier attribute, Identifier modifier, double value,
				AttributeModifier.Operation operation) {
			calls.add(new Call(true, attribute, modifier, value, operation));
		}
	}

	@Test
	void stripRemovesEveryKnownPair() {
		RecordingSink sink = new RecordingSink();

		ClassApplier.stripAll(sink);

		assertTrue(sink.calls.stream().noneMatch(RecordingSink.Call::add));
		assertTrue(sink.calls.stream().anyMatch(c ->
				c.modifier().getPath().equals("warrior_health") && c.attribute().getPath().equals("max_health")));
	}

	@Test
	void applyAddsBasePassives() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);
		RecordingSink sink = new RecordingSink();

		ClassApplier.applyAll(sink, classes.currentDefinition());

		assertTrue(sink.calls.stream().anyMatch(c -> c.add()
				&& c.attribute().getPath().equals("max_health")
				&& c.modifier().getPath().equals("warrior_health")
				&& c.value() == ClassRegistry.WARRIOR_HEALTH_BONUS));
	}

	@Test
	void unknownAttributesAreSkipped() {
		Identifier unknown = Identifier.fromNamespaceAndPath("minecraft", "nope");
		ClassDefinition def = new ClassDefinition(
				ClassRegistry.WARRIOR, "Warrior", false,
				List.of(new Passive(unknown,
						Identifier.fromNamespaceAndPath("infusedclasses", "bad"),
						1.0, AttributeModifier.Operation.ADD_VALUE)),
				ClassRegistry.WARRIOR_ABILITY, 0, "", List.of());
		RecordingSink sink = new RecordingSink();

		ClassApplier.applyAll(sink, def);

		assertEquals(0, sink.calls.size());
	}
}

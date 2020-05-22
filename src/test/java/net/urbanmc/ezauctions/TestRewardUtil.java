package net.urbanmc.ezauctions;

import org.bukkit.entity.Player;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class TestRewardUtil  {

	@Test
	public void test() {
		Player p = mock(Player.class);
		when(p.getName()).thenReturn("Elian");
		assertEquals("Elian", p.getName());
	}
}

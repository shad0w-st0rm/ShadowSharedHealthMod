package me.Shadow;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.Difficulty;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("sharedhealthmod")
@Mod.EventBusSubscriber(modid = "sharedhealthmod", bus=Mod.EventBusSubscriber.Bus.MOD)
public class SharedHealthMod
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    int tickAmount = 0;
    double distance = 0;
    MinecraftServer server;
    DamageSource source;
    Effect absorption;

    public SharedHealthMod() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        //LOGGER.info("HELLO FROM PREINIT");
        //LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        //LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().options);
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        // some example code to dispatch IMC to another mod
        //InterModComms.sendTo("examplemod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        //LOGGER.info("Got IMC {}", event.getIMCStream().
                //map(m->m.getMessageSupplier().get()).
                //collect(Collectors.toList()));
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        server = event.getServer();
        source = new DamageSource("distancedamage").bypassArmor().bypassMagic();
        absorption = Effect.byId(22);
    }
    @SubscribeEvent
    public void onServerStarted(FMLServerStartedEvent event)
    {
    	server.overworld().setDayTime(1512000);
    	server.setDifficulty(Difficulty.HARD, true);
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    /*@Mod.EventBusSubscriber(modid = "sharedhealthmod", bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents
    {
    	
    }*/
    
    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event)
    {
    	if(event.phase == Phase.START)
    	{
    		tickAmount++;
    		PlayerList list = server.getPlayerList();
			List<ServerPlayerEntity> players = list.getPlayers();
			distance = 0;
			for(ServerPlayerEntity player : players)
			{
				player.displayClientMessage(new StringTextComponent(TextFormatting.GOLD + "X: " + TextFormatting.GREEN + player.blockPosition().getX() + TextFormatting.GOLD + " Y: " + TextFormatting.GREEN + player.blockPosition().getY() + TextFormatting.GOLD + " Z: " + TextFormatting.GREEN + player.blockPosition().getZ()), true);
				if(player.hasEffect(absorption))
				{
					EffectInstance effect = player.removeEffectNoUpdate(absorption);
					if(effect != null)
					{
						effect.getEffect().removeAttributeModifiers(player, player.getAttributes(), effect.getAmplifier());
					}
				}
				for(ServerPlayerEntity player2 : players)
    			{
    				if(!player2.getName().equals(player.getName()))
    				{
    					if(player2.distanceTo(player) < 10 && !(player.getHealth() == player.getMaxHealth() || player2.getHealth() == player2.getMaxHealth()) && !(player.getFoodData().getFoodLevel() < 17 || player2.getFoodData().getFoodLevel() < 17) && !(player.getHealth() == 0 || player2.getHealth() == 0))
    					{
    						LivingHealEvent event2 = new LivingHealEvent(player, 0.02f);
    				        MinecraftForge.EVENT_BUS.post(event2);
    				        player.causeFoodExhaustion(0.16f);
    				        player2.causeFoodExhaustion(0.16f);
    				        return;
    					}
    					distance += player2.distanceTo(player);
    				}
    			}
			}
			if(players.size() < 2) return;
			distance /= ((players.size() - 1) * players.size());
			if(distance > 25 && tickAmount % 40 == 0)
			{
				for(ServerPlayerEntity player : players)
				{
					player.hurt(source, 1);
				}
			}
    	}
    }
        
    @SubscribeEvent
    public void onDamage(LivingDamageEvent event)
    {
    	Entity entity = event.getEntity();
    	if(entity instanceof PlayerEntity)
    	{
    		if(event.getSource().toString().equals(source.toString())) return;
    		PlayerList list = server.getPlayerList();
			List<ServerPlayerEntity> players = list.getPlayers();
			float health = ((PlayerEntity) entity).getHealth();
    		
			for(ServerPlayerEntity player : players)
			{
				if(player.getHealth() > health)
				{
					LivingDamageEvent event2 = new LivingDamageEvent(player, source, player.getHealth() - health);
			        MinecraftForge.EVENT_BUS.post(event2);
				}
				player.setHealth(health - event.getAmount());
			}
			
			event.setAmount(0);
    	}
    }
    
    @SubscribeEvent
    public void onHeal(LivingHealEvent event)
    {
    	Entity entity = event.getEntity();
    	if(entity instanceof PlayerEntity)
    	{
    		PlayerList list = server.getPlayerList();
			List<ServerPlayerEntity> players = list.getPlayers();
			float health = ((PlayerEntity) entity).getHealth();
			for(ServerPlayerEntity player : players)
			{
				player.setHealth(health + event.getAmount());
			}
			
			event.setAmount(0);
    	}
    }
    
    @SubscribeEvent
    public void onChat(ServerChatEvent event)
    {
    	String message = event.getMessage();
    	if(message.equals("tick"))
    	{
    		float tick = server.getAverageTickTime();
    		event.setComponent(new StringTextComponent(TextFormatting.AQUA + "Average tick time: " + ((tick < 15) ? TextFormatting.GREEN : (tick < 30) ? TextFormatting.YELLOW : (tick < 45) ? TextFormatting.RED : (tick < 60) ? TextFormatting.DARK_RED : TextFormatting.BLACK) + server.getAverageTickTime()));
    	}
    	
    	else if(message.equals("distance"))
    	{
    		event.setComponent(new StringTextComponent(TextFormatting.DARK_BLUE + "Average Distance: " + ((distance < 10) ? TextFormatting.GREEN : (distance < 20) ? TextFormatting.YELLOW : (distance < 25) ? TextFormatting.RED : TextFormatting.DARK_RED) + distance));
    	}
    }
    
    @SubscribeEvent
    public void onPotionAdded(PotionEvent.PotionAddedEvent event)
    {
    	LivingEntity entity = event.getEntityLiving();
    	if(entity instanceof PlayerEntity)
    	{
    		PlayerEntity player = (PlayerEntity) entity;
    		EffectInstance effect = event.getPotionEffect();
    		PlayerList list = server.getPlayerList();
			List<ServerPlayerEntity> players = list.getPlayers();
			for(ServerPlayerEntity otherPlayer : players)
			{
				if(!otherPlayer.getName().equals(player.getName()))
				{
					if (otherPlayer.canBeAffected(effect))
					{
				         EffectInstance effectinstance = otherPlayer.getActiveEffectsMap().get(effect.getEffect());
				         if (effectinstance == null) {
				        	 otherPlayer.getActiveEffectsMap().put(effect.getEffect(), effect);
				        	 effect.getEffect().addAttributeModifiers(otherPlayer, otherPlayer.getAttributes(), effect.getAmplifier());
				        	 
				        	 otherPlayer.connection.send(new SPlayEntityEffectPacket(otherPlayer.getId(), effect));
				             CriteriaTriggers.EFFECTS_CHANGED.trigger(otherPlayer);
				         } else if (effectinstance.update(effect)) {				        	 
				        	 Effect effect2 = effect.getEffect();
				             effect2.removeAttributeModifiers(otherPlayer, otherPlayer.getAttributes(), effect.getAmplifier());
				             effect2.addAttributeModifiers(otherPlayer, otherPlayer.getAttributes(), effect.getAmplifier());
				             
				             otherPlayer.connection.send(new SPlayEntityEffectPacket(otherPlayer.getId(), effect));
				             CriteriaTriggers.EFFECTS_CHANGED.trigger(otherPlayer);
				         }
					}
				}
			}
    	}
    }
    
    @SubscribeEvent
    public void onPotionRemoved(PotionEvent.PotionRemoveEvent event)
    {
    	LivingEntity entity = event.getEntityLiving();
    	if(entity instanceof PlayerEntity)
    	{
    		PlayerEntity player = (PlayerEntity) entity;
    		EffectInstance effect = event.getPotionEffect();
    		PlayerList list = server.getPlayerList();
			List<ServerPlayerEntity> players = list.getPlayers();
			for(ServerPlayerEntity otherPlayer : players)
			{
				if(!otherPlayer.getName().equals(player.getName()))
				{
					EffectInstance effectinstance = otherPlayer.removeEffectNoUpdate(effect.getEffect());
					if(effectinstance != null)
					{
						effectinstance.getEffect().removeAttributeModifiers(otherPlayer, otherPlayer.getAttributes(), effectinstance.getAmplifier());
					}
				}
			}
    	}
    }
    
    @SubscribeEvent
    public void onPotionExpire(PotionEvent.PotionExpiryEvent event)
    {
    	LivingEntity entity = event.getEntityLiving();
    	if(entity instanceof PlayerEntity)
    	{
    		PlayerEntity player = (PlayerEntity) entity;
    		EffectInstance effect = event.getPotionEffect();
    		PlayerList list = server.getPlayerList();
			List<ServerPlayerEntity> players = list.getPlayers();
			for(ServerPlayerEntity otherPlayer : players)
			{
				if(!otherPlayer.getName().equals(player.getName()))
				{
					EffectInstance effectinstance = otherPlayer.removeEffectNoUpdate(effect.getEffect());
					if(effectinstance != null)
					{
						effectinstance.getEffect().removeAttributeModifiers(otherPlayer, otherPlayer.getAttributes(), effectinstance.getAmplifier());
					}
				}
			}
    	}
    }
    
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event)
    {
    	event.getChunk().setInhabitedTime(360000000);
    }
}

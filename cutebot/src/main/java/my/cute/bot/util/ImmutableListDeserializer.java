package my.cute.bot.util;

import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class ImmutableListDeserializer implements JsonDeserializer<ImmutableList<?>> {

	private static final Logger logger = LoggerFactory.getLogger(ImmutableListDeserializer.class);
	
	@Override
	public ImmutableList<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		JsonArray array;
		try {
			array = json.getAsJsonArray();
		} catch (IllegalStateException e) {
			logger.warn("ImmutableListDeserializer: tried to load json that was not a JsonArray, returning empty list", e);
			return ImmutableList.of();
		}
		ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
		array.forEach(element ->
		{
			builder.add(element.getAsString());
		});
		return builder.build();
	}

}

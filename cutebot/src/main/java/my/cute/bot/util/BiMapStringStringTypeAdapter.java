package my.cute.bot.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class BiMapStringStringTypeAdapter extends TypeAdapter<BiMap<String, String>> {

	//idk why this make so much resource warning??
	@SuppressWarnings("resource")
	@Override
	public void write(JsonWriter out, BiMap<String, String> value) throws IOException {
		
		if (value == null) {
			out.nullValue();
			return;
		}
		
		out.beginObject();
		out.name("size").value(value.size());
		Iterator<Entry<String, String>> iterator = value.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, String> entry = iterator.next();
			out.name("K").value(entry.getKey());
			out.name("V").value(entry.getValue());
		}
		out.endObject();
		
	}

	@Override
	public BiMap<String, String> read(JsonReader in) throws IOException {
		
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		
		in.beginObject();
		in.nextName();
		int size = in.nextInt();
		BiMap<String, String> map = HashBiMap.create(size);
		for(int i=0; i < size; i++) {
			in.nextName();
			String key = in.nextString();
			in.nextName();
			String val = in.nextString();
			map.put(key, val);
		}
		in.endObject();
		return map;
		
	}

}

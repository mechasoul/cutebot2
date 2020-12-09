package my.cute.bot.util;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import gnu.trove.iterator.TObjectLongIterator;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;

public final class TStringLongMapTypeAdapter extends TypeAdapter<TObjectLongMap<String>> {

	@SuppressWarnings("resource")
	@Override
	public void write(JsonWriter out, TObjectLongMap<String> value) throws IOException {
		
		if (value == null) {
			out.nullValue();
			return;
		}
		
		out.beginObject();
		out.name("size").value(value.size());
		TObjectLongIterator<String> iterator = value.iterator();
		while(iterator.hasNext()) {
			iterator.advance();
			out.name("K").value(iterator.key());
			out.name("V").value(iterator.value());
		}
		out.endObject();
	}

	@Override
	public TObjectLongMap<String> read(JsonReader in) throws IOException {
		
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		
		in.beginObject();
		in.nextName();
		int size = in.nextInt();
		TObjectLongMap<String> map = new TObjectLongHashMap<String>(size * 4 / 3, 0.75f, -1);
		for(int i=0; i < size; i++) {
			in.nextName();
			String name = in.nextString();
			in.nextName();
			long val = in.nextLong();
			map.put(name, val);
		}
		in.endObject();
		return map;
	}



}

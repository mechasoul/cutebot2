package my.cute.bot.util;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import gnu.trove.iterator.hash.TObjectHashIterator;
import gnu.trove.set.hash.THashSet;

public final class THashSetStringTypeAdapter extends TypeAdapter<THashSet<String>> {

	@SuppressWarnings("resource")
	@Override
	public void write(JsonWriter out, THashSet<String> value) throws IOException {
		
		if (value == null) {
			out.nullValue();
			return;
		}
		
		out.beginObject();
		out.name("size").value(value.size());
		TObjectHashIterator<String> iterator = value.iterator();
		while(iterator.hasNext()) {
			out.name("E").value(iterator.next());
		}
		out.endObject();
		
	}

	@Override
	public THashSet<String> read(JsonReader in) throws IOException {
		
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		
		in.beginObject();
		in.nextName();
		int size = in.nextInt();
		THashSet<String> set = new THashSet<String>(size * 4 / 3, 0.75f);
		for(int i=0; i < size; i++) {
			in.nextName();
			set.add(in.nextString());
		}
		in.endObject();
		return set;
		
	}

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dr.radio.backend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Iterator over et JSONArray.
 * Brug:
 * JSONArray jsonArr = ...;
 * for (JSONObject elem : new JSONIterator(jsonArr)) { ... }
 *
 * @author Jacob Nordfalk
 */
public class JSONArrayIterator implements Iterable<JSONObject>, Iterator<JSONObject> {
  private int n = 0;
  private final JSONArray jsonArr;

  public JSONArrayIterator(JSONArray jsonArray) {
    jsonArr = jsonArray;
  }

  public Iterator<JSONObject> iterator() {
    return this;
  }

  @Override
  public boolean hasNext() {
    return jsonArr!=null && n < jsonArr.length();
  }

  @Override
  public JSONObject next() {
    return jsonArr.optJSONObject(n++);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Ikke understøttet");
  }
}

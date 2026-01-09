package com.developingstorm.games.sad.util.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class JsonParser {
  StringBuilder builder;
  static final String NUMERIC = "0123456789-.";
  
  enum TokenType {BEGIN_CURLY, END_CURLY, BEGIN_ARRAY, END_ARRAY, COMMA, KEYSEP, NULL, TRUE, FALSE, STRING, NUMBER, END}
  

  
  private static class Scanner {
    private char[] buf;
    int pos;
    public Scanner(String val) {
      buf = val.toCharArray();
      pos = 0;
    }
    
    
    private static boolean isWS(char c) {
      return (c == ' ' || c == '\r' || c == '\n' || c == '\t');
    }
    
    private boolean eof() {
      return this.pos >= this.buf.length;
    }
    
    private void eatWS() {
      while (isWS(current()) && !eof()) { 
        this.pos++;
      }
    }

    private void eat(int n) {
      for (int x = 0; x < n; x++) {
        if (eof()) {
          return;
        }
        this.pos++;
      }
    }

    
    char current() {
      return this.buf[this.pos];
    }
    
    TokenType peek() {
      eatWS();
      if (eof()) {
        return TokenType.END;
      }
      switch(current()) {
      case '{':
        return TokenType.BEGIN_CURLY;
      case '}':
        return TokenType.END_CURLY;
      case '[':
        return TokenType.BEGIN_ARRAY;
      case ']':
        return TokenType.END_ARRAY;
      case ':':
        return TokenType.KEYSEP;
      case '"':
        return TokenType.STRING;
      case 'n':
        return TokenType.NULL;
      case ',':
        return TokenType.COMMA;
      case 't':
        return TokenType.TRUE;
      case 'f':
        return TokenType.FALSE;
      default:
        return TokenType.NUMBER;
      }
    }
    
    void consume(TokenType t) {
      eatWS();
      switch(t) {
      case TRUE:
        eat(4);
        break;
      case FALSE:
        eat(5);
        break;
      case NULL:
        eat(4);
        break;
      case COMMA:
      case KEYSEP:
      case BEGIN_ARRAY:
      case END_ARRAY:
      case BEGIN_CURLY:
      case END_CURLY:
        eat(1);
        break;
      case STRING:
        getString();
      case NUMBER:
        getNumber();
      default:
        break;
      }
    }
    
    
    String getString() {
      eat(1);
      boolean escaped = false;
      StringBuilder sb = new StringBuilder();
      while (!eof()) { 
        char c = current();
        eat(1);
        if (c == '"' && escaped == false) {
          return sb.toString();
        } else if (c == '\\') {
          escaped = true;
          continue;
        }
        sb.append(c);
      }
      return sb.toString();
    }
      
    @SuppressWarnings("boxing")
    Number getNumber() {
      StringBuilder sb = new StringBuilder();
      while (!eof()) {
        char c = current();
        if (NUMERIC.indexOf(c) >= 0) {
          sb.append(c);
          eat(1);
        } else {
          break;
        }
        
      }
      String v = sb.toString();
      
      try {
        return Long.parseLong(v);
      }catch (NumberFormatException e) {
        return Double.parseDouble(v);
      }
    }
  }
  
  Scanner scanner;
  
  public JsonParser(String val) {
    scanner = new Scanner(val);
    
  }
  
  
  public Object parse() {
    TokenType tt = this.scanner.peek();
    switch(tt) {
    case BEGIN_CURLY:
      return parseJsonObj();
    case STRING:
      return this.scanner.getString();
    case BEGIN_ARRAY:
      return parseJsonArray();
    case NUMBER:
      return this.scanner.getNumber();
    default:
      throw new RuntimeException("Invalid JSON");
    }
    
  }
  

  @SuppressWarnings("boxing")
  private Object parseJsonArray() {

    List<Object> list = new ArrayList<Object>();
    this.scanner.consume(TokenType.BEGIN_ARRAY);
    while (true) {
      TokenType tt = this.scanner.peek();
      switch(tt) {
      case BEGIN_CURLY:
        list.add(parseJsonObj());
        break;
      case STRING:
        list.add(this.scanner.getString());
        break;
      case BEGIN_ARRAY:
        list.add(parseJsonArray());
        break;
      case NUMBER:
        list.add(this.scanner.getNumber());
        break;
      case NULL:
        list.add(null);
        this.scanner.consume(tt);
        break;
      case TRUE:
        list.add(true);
        this.scanner.consume(tt);
        break;
      case FALSE:
        list.add(false);
        this.scanner.consume(tt);
        break;
      case COMMA:
        this.scanner.consume(tt);
        break;
      case END_ARRAY:
        this.scanner.consume(tt);
        return list.toArray();
      default:
        throw new RuntimeException("BAD ARRAY");
      }
    }

  }


  private Object parseJsonObj() {
    JsonObj obj = new JsonObj();
    this.scanner.consume(TokenType.BEGIN_CURLY);
    String key = null;
    while (true) {
      TokenType tt = this.scanner.peek();
      switch(tt) {
      case BEGIN_ARRAY:
        if (key == null) {
          throw new RuntimeException("Unexpected Array");
        }
        obj.put(key, parseJsonArray());
        key = null;
        break;
      case STRING:
        String s = this.scanner.getString();
        if (key == null) {
          key = s;
        } else {
          obj.put(key, s);
          key = null;
        }
        break;
      case BEGIN_CURLY:
        if (key == null) {
          throw new RuntimeException("Unexpected obj");
        }
        obj.put(key, parseJsonObj());
        key = null;
        break;
      case NUMBER:
        if (key == null)  {
          throw new RuntimeException("Unexpected number");
        } 
        Number num = this.scanner.getNumber();
        obj.put(key, num);
        key = null;
        break;
      case NULL:
        if (key == null)  {
          throw new RuntimeException("Unexpected null");
        } 
        obj.put(key, null);
        key = null;
        this.scanner.consume(tt);
        break;
      case TRUE:
        if (key == null)  {
          throw new RuntimeException("Unexpected bool");
        } 
        obj.put(key, true);
        key = null;
        this.scanner.consume(tt);
        break;
      case FALSE:
        if (key == null)  {
          throw new RuntimeException("Unexpected bool");
        } 
        obj.put(key, true);
        key = null;
        this.scanner.consume(tt);
        break;
      case COMMA:
        this.scanner.consume(tt);
        break;
      case KEYSEP:
        this.scanner.consume(tt);
        break;
      case END_CURLY:
        this.scanner.consume(tt);
        return obj;
      default:
        throw new RuntimeException("BAD OBJECT");
      }
    }
  }


  public static Object parse(String val) {
    JsonParser p = new JsonParser(val);
    return p.parse();
  }
  
  
  public static void main(String[] args) {
    JsonObj obj = (JsonObj) parse("{\"a\":10, \"b\" : [1, 2.2, -3], \"c\" : true, \"d\" : false , \"e\" : null, \"f\" : {}, \"g\" : [], \"h\" : \"VAL\", \"i\" : {\"KEY1\" : \"VAL1\", \"KEY2\" : \"VAL2\"}}");

    JsonObj obj2 = obj.getObj("i");
    String val2 = obj2.getString("KEY2");

    

    
    
  }
  
 
}

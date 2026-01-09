package com.developingstorm.games.sad.persistence;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.orders.DirectionalMove;
import com.developingstorm.games.sad.orders.Move;
import com.developingstorm.games.sad.util.json.JsonObj;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializes and deserializes Unit objects to/from JSON.
 */
public class UnitSerializer {

    /**
     * Serializes a unit to JSON.
     *
     * @param unit the unit to serialize
     * @return JSON object containing unit state
     */
    public static JsonObj serializeUnit(Unit unit) {
        JsonObj json = new JsonObj();

        // Basic identity
        json.put("id", unit.id);
        json.put("type", unit.getType().name());
        json.put("name", unit.name);

        // Ownership and location
        json.put("ownerIndex", unit.getOwner().id);
        JsonObj locJson = new JsonObj();
        locJson.put("x", unit.getLocation().x);
        locJson.put("y", unit.getLocation().y);
        json.put("location", locJson);

        // State
        json.put("hits", unit.life.hits);
        json.put("dist", unit.dist);

        // Carrying units
        List<Unit> carrying = unit.carries;
        if (carrying != null && !carrying.isEmpty()) {
            Object[] carryingIds = new Object[carrying.size()];
            for (int i = 0; i < carrying.size(); i++) {
                carryingIds[i] = carrying.get(i).id;
            }
            json.put("carrying", carryingIds);
        }

        // Unit being carried by
        Unit onboard = unit.onboard;
        if (onboard != null) {
            json.put("onboard", onboard.id);
        }

        // Order (if any)
        Order order = unit.getOrder();
        if (order != null) {
            json.put("orderType", order.getType().name());

            // Serialize order-specific data
            JsonObj orderData = serializeOrderData(order);
            if (orderData != null && orderData.keySet().size() > 0) {
                json.put("orderData", orderData);
            }
        }

        return json;
    }

    /**
     * Serializes order-specific data based on order type.
     * Uses reflection to access protected fields in order classes.
     *
     * @param order the order to serialize
     * @return JSON object with order-specific data, or null if no data needed
     */
    private static JsonObj serializeOrderData(Order order) {
        JsonObj orderData = new JsonObj();

        try {
            if (order instanceof Move) {
                // For Move orders (including HeadHome which extends Move)
                // Serialize the destination location
                Field locField = Move.class.getDeclaredField("loc");
                locField.setAccessible(true);
                Location loc = (Location) locField.get(order);

                if (loc != null) {
                    JsonObj locJson = new JsonObj();
                    locJson.put("x", loc.x);
                    locJson.put("y", loc.y);
                    orderData.put("destination", locJson);
                }
            }

            if (order instanceof DirectionalMove) {
                // For DirectionalMove orders
                Field dirField = DirectionalMove.class.getDeclaredField("dir");
                dirField.setAccessible(true);
                Direction dir = (Direction) dirField.get(order);

                if (dir != null) {
                    orderData.put("direction", dir.name());
                }
            }
        } catch (Exception e) {
            // If reflection fails, log but don't crash - order will be lost but game can continue
            System.err.println(
                "Warning: Failed to serialize order data for " +
                    order.getType() +
                    ": " +
                    e.getMessage()
            );
        }

        return orderData;
    }

    /**
     * Deserializes a unit from JSON.
     * Note: This creates the unit but relationships (carrying, onboard) must be
     * reconstructed separately after all units are loaded.
     *
     * @param json the JSON object
     * @return partially reconstructed unit data
     */
    public static UnitData deserializeUnit(JsonObj json) {
        UnitData data = new UnitData();

        data.id = json.getLong("id");
        data.typeName = json.getString("type");
        data.name = json.getString("name");
        data.ownerIndex = json.getInteger("ownerIndex");

        JsonObj locJson = json.getObj("location");
        data.x = locJson.getInteger("x");
        data.y = locJson.getInteger("y");

        data.hits = json.getInteger("hits");
        data.dist = json.getInteger("dist");

        // Extract carrying IDs (to be resolved later)
        Object[] carryingArray = json.getArray("carrying");
        if (carryingArray != null) {
            data.carryingIds = new ArrayList<>();
            for (Object obj : carryingArray) {
                data.carryingIds.add((Long) obj);
            }
        }

        // Extract onboard ID (to be resolved later)
        Long onboardId = json.getLong("onboard");
        if (onboardId != null) {
            data.onboardId = onboardId;
        }

        // Extract order type
        String orderType = json.getString("orderType");
        if (orderType != null) {
            data.orderType = orderType;

            // Extract order-specific data
            JsonObj orderData = json.getObj("orderData");
            if (orderData != null) {
                data.orderData = orderData;
            }
        }

        return data;
    }

    /**
     * Intermediate data structure for unit deserialization.
     * Contains all unit data including unresolved references.
     */
    public static class UnitData {

        public long id;
        public String typeName;
        public String name;
        public int ownerIndex;
        public int x;
        public int y;
        public int hits;
        public int dist;
        public List<Long> carryingIds;
        public Long onboardId;
        public String orderType;
        public JsonObj orderData;
    }
}

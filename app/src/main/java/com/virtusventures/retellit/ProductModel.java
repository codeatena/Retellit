package com.virtusventures.retellit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProductModel {

    public String brand;
    public String productname;
    public String product_size;
    public String price;
    public String product_type;
    public String product_image;
    public String long_description;
    public String ingredients;
    public String instructions;
    public String benefits;
    public String product_content;
    public List<String> options = new ArrayList<>();
    public float star;

    public ProductModel(JSONObject jsonObject)
    {
        try {
            this.productname = jsonObject.getString("productname");
            this.product_image = jsonObject.getString("image");
            this.brand = jsonObject.getString("segmentation");
            this.product_size = jsonObject.getString("size");;
            this.price = jsonObject.getString("price");;
            this.product_type = jsonObject.getString("type");;
            this.long_description = jsonObject.getString("long_description");;
            this.ingredients = jsonObject.getString("ingredients");;
            this.instructions = jsonObject.getString("instructions");;
            this.benefits = jsonObject.getString("benefits");;
            this.product_content = jsonObject.getString("content");

            JSONObject optionJson = jsonObject.getJSONObject("Options");
            JsonParser jsonParser = new JsonParser();
            JsonObject gsonObject = (JsonObject)jsonParser.parse(optionJson.toString());
            Set <Map.Entry<String ,JsonElement>> entries = gsonObject.entrySet();
            for (Map.Entry<String ,JsonElement> entry : entries){
                options.add(entry.getValue().getAsString());
            }
            this.star = Float.valueOf(jsonObject.getString("star"));

        }catch (Exception e){

        }
    }
}

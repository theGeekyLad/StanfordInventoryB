package com.thegeekylad.inventoryb;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.getbase.floatingactionbutton.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import zxing.IntentIntegrator;
import zxing.IntentResult;

public class InventoryActivity extends AppCompatActivity {

    private static final String API_URL_1 = "http://34.210.77.132:3003/psr/demouiddetails";
    private static final String API_URL_2 = "";

    private View refView;
    private LinearLayout itemsLayout;
    private ArrayList<View> list;
    private float sum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Items List");
        setSupportActionBar(toolbar);

        // init
        itemsLayout = (LinearLayout) findViewById(R.id.itemLayout);
        list = new ArrayList<View>();

        // fab group
        FloatingActionsMenu menuMultipleActions = (FloatingActionsMenu) findViewById(R.id.fab);

        // fab | barcode scanner
        FloatingActionButton actionBarcode = new FloatingActionButton(getBaseContext());
        actionBarcode.setColorNormal(Color.parseColor("#FF3F51B5"));
        actionBarcode.setColorPressed(Color.parseColor("#FFFF4081"));
        actionBarcode.setTitle("Scan");
        actionBarcode.setIconDrawable(getResources().getDrawable(R.drawable.ic_open_in_new_white_24dp, null));
        actionBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refView = v;
                IntentIntegrator integrator = new IntentIntegrator(InventoryActivity.this);
                integrator.initiateScan();
            }
        });
        menuMultipleActions.addButton(actionBarcode);

        // fab | submit
        FloatingActionButton actionSubmit = new FloatingActionButton(getBaseContext());
        actionSubmit.setColorNormal(Color.parseColor("#FF3F51B5"));
        actionSubmit.setColorPressed(Color.parseColor("#FFFF4081"));
        actionSubmit.setTitle("Submit");
        actionSubmit.setIconDrawable(getResources().getDrawable(R.drawable.ic_navigate_next_white_24dp, null));
        actionSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // #2 | api request | submit info
                for (int i=0; i<list.size(); i++) {
                    View itemView = list.get(i);
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("uid", ((TextView)itemView.findViewById(R.id.uid)).getText().toString());
                        jsonObject.put("quantity", ((TextView)itemView.findViewById(R.id.quantity)).getText().toString());
                    } catch (JSONException je) {
                        Toast.makeText(InventoryActivity.this, je.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                            Request.Method.POST,
                            API_URL_2,
                            jsonObject,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {

                                    // success / failure

                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Toast.makeText(InventoryActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                    Volley.newRequestQueue(InventoryActivity.this).add(jsonObjectRequest);
                }
            }
        });
        menuMultipleActions.addButton(actionSubmit);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        try {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (scanResult != null) {

                // product uid
                final String uid = scanResult.getContents();

                // uid already scanned
                for (int i=0; i<list.size(); i++) {
                    View itemView = list.get(i);
                    if (((TextView) itemView.findViewById(R.id.uid)).getText().toString().equals(uid)) {
                        int q = Integer.parseInt(((EditText) itemView.findViewById(R.id.quantity)).getText().toString())+1;
                        ((EditText) itemView.findViewById(R.id.quantity)).setText(q+"");
                        updateTotal();
                        return;
                    }
                }

                // #1 | api request | get info
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("uid", uid);
                CustomJsonRequest customJsonRequest= new CustomJsonRequest(
                        Request.Method.POST,
                        API_URL_1,
                        jsonObject,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {

                                // db | uid absent
                                if (response.length() == 0) {
                                    new AlertDialog.Builder(InventoryActivity.this)
                                            .setTitle("Unknown Item")
                                            .setMessage(uid+" doesn't exist in the  database. Kindly scan a different product.")
                                            .create()
                                            .show();
                                    return;
                                }

                                // parsing response
                                JSONObject jsonObject1=null;
                                try {
                                    jsonObject1 = (JSONObject) response.get(0);
                                } catch (JSONException je) {
                                    Toast.makeText(InventoryActivity.this, je.getMessage(), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                String name, price, quantity;
                                try {
                                    name = jsonObject1.getString("name");
                                    price = jsonObject1.getString("price");
                                    quantity = jsonObject1.getString("quantity");
                                } catch (Exception je) {
                                    Toast.makeText(InventoryActivity.this, je.getMessage(), Toast.LENGTH_LONG).show();
                                    return;
                                }

                                // update ui | + ~ list
                                View itemView = getLayoutInflater().inflate(R.layout.layout_item, null);
                                ((EditText) itemView.findViewById(R.id.quantity)).setOnKeyListener(new View.OnKeyListener() {
                                    @Override
                                    public boolean onKey(View view, int i, KeyEvent keyEvent) {
                                        updateTotal();
                                        return false;
                                    }
                                });
                                ((TextView) itemView.findViewById(R.id.uid)).setText(uid); // uid
                                ((TextView) itemView.findViewById(R.id.name)).setText(name); // name
                                ((TextView) itemView.findViewById(R.id.price)).setText(price); // price
                                ((TextView) itemView.findViewById(R.id.quantity)).setText(quantity); // quantity
                                sum += Float.parseFloat(price) * Float.parseFloat(quantity);
                                ((TextView)findViewById(R.id.total)).setText(sum + "");
                                list.add(itemView);
                                itemsLayout.addView(itemView);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(InventoryActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                );
                Volley.newRequestQueue(InventoryActivity.this).add(customJsonRequest);
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotal() {
        sum = 0;
        for (int j=0; j<list.size(); j++) {
            View itemView = list.get(j);
            sum += Float.parseFloat(((TextView)itemView.findViewById(R.id.price)).getText().toString())
                    * Integer.parseInt((((EditText)itemView.findViewById(R.id.quantity)).getText().toString().length() == 0)?"0":((EditText)itemView.findViewById(R.id.quantity)).getText().toString());
        }
        ((TextView) findViewById(R.id.total)).setText(sum + "");
    }

    public class CustomJsonRequest extends JsonRequest<JSONArray> {

        public CustomJsonRequest(
                int method,
                String url,
                JSONObject jsonObject,
                Response.Listener<JSONArray> responseListener,
                Response.ErrorListener errorListener) {
            super(method, url, jsonObject.toString(), responseListener, errorListener);
        }

        @Override
        protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
            try {
                String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                return Response.success(new JSONArray(json), HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                Toast.makeText(InventoryActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                return Response.error(new ParseError(e));
            }
        }
    }

}
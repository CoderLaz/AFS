package app.netlify.benlazinnovatives.anyformulasolver.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;

import app.netlify.benlazinnovatives.anyformulasolver.R;
import app.netlify.benlazinnovatives.anyformulasolver.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private final String[] params = {"q", "th", "tc", "ro", "ri", "k", "l", "h", "cdr", "cvr", "tr", "ct"};
    private SharedPreferences sp;
    private LinkedHashMap<String, Float> map = new LinkedHashMap<>();
    int[] ids = {R.id.editTextNumber, R.id.editTextNumber1, R.id.editTextNumber2, R.id.editTextNumber3, R.id.editTextNumber4, R.id.editTextNumber5, R.id.editTextNumber6, R.id.editTextNumber7};
    EditText et;
    ImageView imgOne, imgTwo;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sp = requireActivity().getSharedPreferences("sp", MODE_PRIVATE);
        if (!sp.contains("q")) {
            SharedPreferences.Editor ed = sp.edit();
            ed.putString("q", "Q<br>in<br>W");
            ed.putString("th", "T<sub>h</sub><br>in<br>&#x00B0;C or K");
            ed.putString("tc", "T<sub>c</sub><br>in<br>&#x00B0;C or K");
            ed.putString("ro", "r<sub>o</sub><br>in<br>m");
            ed.putString("ri", "r<sub>i</sub><br>in<br>m");
            ed.putString("k", "k<br>in<br>W / m K");
            ed.putString("l", "l<br>in<br>m");
            ed.putString("h", "h<br>in<br>W / m<sup>2</sup>K");
            ed.putString("cdr", "Conductive<br>Resistance");
            ed.putString("cvr", "Convective<br>Resistance");
            ed.putString("tr", "Total<br>Resistance");
            ed.putString("ct", "Critical<br>Thickness<br>(Theory)");
            ed.apply();
        }

        Button saveBtn = root.findViewById(R.id.btn_save);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                et = root.findViewById(R.id.editTextNumber);
                if (et.getText().toString().equals(""))
                    Toast.makeText(getActivity(), "Nothing to save..!!!", Toast.LENGTH_SHORT).show();
            }
        });

        Button delBtn = root.findViewById(R.id.btn_delete);
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delete();
            }
        });

        Button graphBtn = root.findViewById(R.id.btn_graph);
        graphBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    graph(root);
                }
                catch(Exception e) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Error")
                            .setMessage(e.toString())
                            .setNegativeButton("OK", dialogClickListener).show();
                }
            }
        });

        int[] pids = {R.id.p1, R.id.p2, R.id.p3, R.id.p4, R.id.p5, R.id.p6, R.id.p7, R.id.p8, R.id.p9, R.id.p10, R.id.p11, R.id.p12};

        for (int i = 0; i < params.length; ++i) {
            TextView tv = root.findViewById(pids[i]);
            tv.setText(Html.fromHtml(sp.getString(params[i], "")));
        }

        EditText lastEt = root.findViewById(ids[7]);

        lastEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    if (!map.isEmpty())
                        map.clear();

                    int unknowns = 0;
                    for (int i = 1; i <= 7; ++i) {
                        et = root.findViewById(ids[i]);
                        if (et.getText().toString().equals(""))
                            unknowns += 1;
                    }

                    if (unknowns == 0)
                        soln(root);
                    else {
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Insufficient Input")
                                .setMessage(Html.fromHtml("All the inputs to be entered except 'q' but " + unknowns + " value's is/are blank...</i>"))
                                .setNegativeButton("OK", dialogClickListener).show();
                    }
                }
                return false;
            }
        });
        return root;
    }

    @SuppressLint("SetTextI18n")
    public void soln(View root) {
        for (int i = 1; i <= 7; ++i) {
            et = root.findViewById(ids[i]);
            try {
                map.put(params[i], Float.parseFloat(et.getText().toString()));

                if (i == 7) {
                    double q = (g("th") - g("tc")) / ((Math.log(g("ro") / g("ri")) / (2 * Math.PI * g("k") * g("l"))) + (1 / (2 * Math.PI * g("h") * g("ro") * g("l"))));
                    double cdr = Math.log(g("ro") / g("ri")) / (2 * Math.PI * g("k") * g("l"));
                    double cvr = 1 / (2 * Math.PI * g("h") * g("ro") * g("l"));
                    double tr = cdr + cvr;
                    double ct = g("k") / g("h");
                    double[] arr = {q, cdr, cvr, tr, ct};

                    et = root.findViewById(R.id.editTextNumber);
                    et.setText("q = " + Math.round(q * 10000d) / 10000d + " W");
                    et.setTextColor(Color.parseColor("#1A9B00"));
                    et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                    et.setTypeface(null, Typeface.BOLD);

                    NestedScrollView nsv = root.findViewById(R.id.nestedScrollView);

                    nsv.post(new Runnable() {
                        @Override
                        public void run() {
                            nsv.scrollTo(0, et.getBottom());
                        }
                    });

                    Button saveBtn = root.findViewById(R.id.btn_save);
                    saveBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            save(arr);
                        }
                    });
                }
            }
            catch (NumberFormatException nfe) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Invalid Input")
                        .setMessage(Html.fromHtml("Non numerical input entered for <b>" + params[i] + "</b> is invalid..."))
                        .setNegativeButton("OK", dialogClickListener).show();
            }
            catch (Exception e) {
                et = root.findViewById(R.id.editTextNumber);
                et.setText("Error: " + e.toString());
                et.setTextColor(Color.parseColor("#FFD50000"));
                et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                et.setTypeface(null, Typeface.BOLD);

                NestedScrollView nsv = root.findViewById(R.id.nestedScrollView);

                nsv.post(new Runnable() {
                    @Override
                    public void run() {
                        nsv.scrollTo(0, et.getBottom());
                    }
                });
            }
        }
    }

    public void save(double[] arr) {
        SharedPreferences.Editor ed = sp.edit();
        for (String s : params) {
            boolean condition = s.equals("th") || s.equals("tc") || s.equals("ro") || s.equals("ri") || s.equals("k") || s.equals("l") || s.equals("h");

            if (s.equals("q"))
                ed.putString(s, sp.getString(s, "") + "<br>" + Math.round(arr[0] * 10000d) / 10000d);
            else if (condition)
                ed.putString(s, sp.getString(s, "") + "<br>" + Math.round(g(s) * 10000f) / 10000f);
            else {
                switch(s) {
                    case "cdr": ed.putString(s, sp.getString(s, "") + "<br>" + Math.round(arr[1] * 10000d) / 10000d);
                                break;
                    case "cvr": ed.putString(s, sp.getString(s, "") + "<br>" + Math.round(arr[2] * 10000d) / 10000d);
                                break;
                    case "tr": ed.putString(s, sp.getString(s, "") + "<br>" + Math.round(arr[3] * 10000d) / 10000d);
                                break;
                    case "ct": ed.putString(s, sp.getString(s, "") + "<br>" + Math.round(arr[4] * 10000d) / 10000d);
                                break;
                }
            }
        }
        ed.apply();

        refresh();
    }

    public void delete() {
        if (sp.getString("q", "").length() == 12)
            Toast.makeText(getActivity(), "No Saved values to delete..!!!", Toast.LENGTH_SHORT).show();
        else {
            SharedPreferences.Editor ed = sp.edit();
            ed.clear();
            ed.apply();
            refresh();
        }
    }

    public void refresh() {
        HomeFragment homeFragment = new HomeFragment();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, homeFragment, homeFragment.getTag())
                .commit();
    }

    public Float g(String param) {
        return map.get(param);
    }

    public void graph(View root) {
        if (!Python.isStarted())
            Python.start(new AndroidPlatform(requireActivity()));

        Python py = Python.getInstance();
        PyObject pyobj = py.getModule("main");

        pyobj.callAttr("clear");

        LinearLayout linearLayout = root.findViewById(R.id.linearlayout);

        if (imgOne != null) {
            linearLayout.removeView(imgOne);
            linearLayout.removeView(imgTwo);
        }

        if (sp.getString("q", "").length() > 12) {
            String x = sp.getString("ro", "").substring(24);
            String y = sp.getString("q", "").substring(12);
            String y1 = sp.getString("cdr", "").substring(24);
            String y2 = sp.getString("cvr", "").substring(24);
            String y3 = sp.getString("tr", "").substring(19);

            PyObject img = pyobj.callAttr("main", x, y, y1, y2, y3);

            String imgOneStr = img.toString().split("&&division&&")[0];
            String imgTwoStr = img.toString().split("&&division&&")[1];

            byte[] dataOne = Base64.decode(imgOneStr, Base64.DEFAULT);

            Bitmap bOne = BitmapFactory.decodeByteArray(dataOne, 0, dataOne.length);

            imgOne = new ImageView(getActivity());
            LinearLayout.LayoutParams imgOneParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            imgOneParams.setMargins(0, 0, 0, getDp(35));
            imgOne.setLayoutParams(imgOneParams);
            imgOne.setImageBitmap(bOne);

            linearLayout.addView(imgOne);

            byte[] dataTwo = Base64.decode(imgTwoStr, Base64.DEFAULT);

            Bitmap bTwo = BitmapFactory.decodeByteArray(dataTwo, 0, dataTwo.length);

            imgTwo = new ImageView(getActivity());
            LinearLayout.LayoutParams imgTwoParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            imgTwoParams.setMargins(0, 0, 0, getDp(35));
            imgTwo.setLayoutParams(imgTwoParams);
            imgTwo.setImageBitmap(bTwo);

            linearLayout.addView(imgTwo);

//            imgOne.setImageURI(imgUri);
//            imgOne.setBackgroundColor(Color.parseColor("#E9CDED"));
//            .addView(imgOne);
        }
        else
            Toast.makeText(getActivity(), "No Saved values to plot the graphs..!!!", Toast.LENGTH_SHORT).show();
    }

    public int getDp(float val) {
        float density = requireActivity().getResources().getDisplayMetrics().density;
        return (int) (val * density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
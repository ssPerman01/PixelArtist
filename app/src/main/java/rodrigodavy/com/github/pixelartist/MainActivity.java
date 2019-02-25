package rodrigodavy.com.github.pixelartist;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pixelartist.utils.Constants;
import pixelartist.view.adapter.ColorBoxAdapter;

import static pixelartist.utils.Constants.SPLIT_REGEX;
import static pixelartist.utils.Constants.ZOOM_MAX;
import static pixelartist.utils.Constants.ZOOM_MIN;

public class MainActivity extends AppCompatActivity {

    String TAG = this.getClass().getSimpleName();

    @BindView(R.id.paper_linear_layout)     LinearLayout paper;
    @BindView(R.id.tv_size_broad)           TextView tvSizeBroad;
    @BindView(R.id.v_curr_color)            View vCurrColorSelected;
    @BindView(R.id.view_color)              RecyclerView rcvColor;
    @BindView(R.id.drawer_layout)           DrawerLayout drawerLayout;

    private static final String SETTINGS_GRID   = "grid";
    private static final String SETTINGS_COLORS = "colors";
    private static final String URL_ABOUT = "https://github.com/ssPerman01/PixelArtist/blob/master/README.md";

    private static final int MY_REQUEST_WRITE_STORAGE   = 5;
    private static final int RC_ADD_COLOR               = 1;

    private final ArrayList<DrawerMenuItem> listMenuItem = new ArrayList<>();
    private int currentColor;
    private List<Integer> arrColor;
    private ActionBarDrawerToggle drawerToggle;
    private SharedPreferences settings;
    private boolean grid;
    private boolean isOpenDrawerToggle = false;

    private ColorBoxAdapter colorBoxAdapter;

    /**
     * Converts a file to a content uri, by inserting it into the media store.
     * Requires this permission: <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
     */
    protected static Uri convertFileToContentUri(Context context, File file) throws Exception {

        //Uri localImageUri = Uri.fromFile(localImageFile); // Not suitable as it's not a content Uri

        ContentResolver cr = context.getContentResolver();
        String imagePath = file.getAbsolutePath();
        String uriString = Media.insertImage(cr, imagePath, null, null);
        return Uri.parse(uriString);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);

        ButterKnife.bind(this);
        zoomUtils = new ZoomUtils();

        settings = getPreferences(0);

//        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                updateDrawerHeader();
                isOpenDrawerToggle = true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                isOpenDrawerToggle = false;
            }
        };

        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerLayout.addDrawerListener(drawerToggle);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        ListView leftDrawer = findViewById(R.id.left_drawer);

        addDrawerItems();

        DrawerMenuItemAdapter adapter = new DrawerMenuItemAdapter(this, listMenuItem);
        leftDrawer.setAdapter(adapter);

        leftDrawer.setOnItemClickListener((adapterView, view, i, l) -> listMenuItem.get(i).execute());

        initPalette();

        if (haveTempFile()) {
            openFile(Constants.FILE_TEMP_EXTENSION, false);
        }
        else {
            initPixelGird();
            fillScreen(ContextCompat.getColor(MainActivity.this, R.color.white));


            grid = settings.getBoolean(SETTINGS_GRID, true);

            if (!grid) {
                grid = true;
                pixelGrid();
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    protected void onStop() {
        super.onStop();

        saveFile(Constants.FILE_TEMP_EXTENSION, false);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SETTINGS_GRID, grid);
        editor.apply();
    }

    //Applying changes made in the ColorSelector activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RC_ADD_COLOR) {
            if (resultCode == Activity.RESULT_OK) {
                int c = data.getIntExtra("color", 0);
                arrColor.add(c);
                colorBoxAdapter.notifyDataSetChanged();
                saveColors();
            }
        }
    }

    long lastTimeBackPress = 0;
    @Override
    public void onBackPressed() {
        if (isOpenDrawerToggle) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else {
            if (lastTimeBackPress == 0 || System.currentTimeMillis() - lastTimeBackPress > 1500) {
                showToast(R.string.toast_confirm_quit, Toast.LENGTH_LONG);
            }
            else {
                super.onBackPressed();
            }
            lastTimeBackPress = System.currentTimeMillis();
        }

    }

    private void addDrawerItems() {
        DrawerMenuItem drawerNew = new DrawerMenuItem(R.drawable.menu_new, R.string.menu_new) {
            @Override
            public void execute() {
                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
//                final View v = findViewById(R.id.color_button_white);

                alertDialog.setTitle(getString(R.string.alert_dialog_title_new));
                alertDialog.setMessage(getString(R.string.alert_dialog_message_new));
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                        (dialog, which) -> {
                            dialog.dismiss();
                            fillScreen(ContextCompat.getColor(MainActivity.this, R.color.white));
                            updateDrawerHeader();
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
            }
        };

        DrawerMenuItem drawerOpen = new DrawerMenuItem(R.drawable.menu_open, R.string.menu_open) {
            @Override
            public void execute() {
                File path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                if ((path != null) && (path.listFiles().length > 0)) {
                    File[] files = path.listFiles();

                    List<CharSequence> list = new ArrayList<>();

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.menu_open);

                    for (File file : files) {
                        if (file.getName().contains(Constants.FILE_EXTENSION)) {
                            list.add(0, file.getName().replace(Constants.FILE_EXTENSION, ""));
                        }
                    }

                    final CharSequence[] charSequences = list.toArray(new CharSequence[0]);

                    builder.setItems(charSequences, (dialogInterface, i) -> {
                        openFile(charSequences[i].toString() + Constants.FILE_EXTENSION, true);
                        updateDrawerHeader();
                    });
                    builder.show();
                }
                else {
                    showToast(R.string.file_no_files_found, Toast.LENGTH_LONG);
                }
            }
        };

        DrawerMenuItem drawerSave = new DrawerMenuItem(R.drawable.menu_save, R.string.menu_save) {
            @Override
            public void execute() {
                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                LayoutInflater layoutInflater = MainActivity.this.getLayoutInflater();

                alertDialog.setTitle(getString(R.string.menu_save));
                alertDialog.setView(layoutInflater.inflate(R.layout.dialog_save, null));
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                        (dialog, which) -> {
                            dialog.dismiss();
                            EditText editText = alertDialog.findViewById(R.id.dialog_filename_edit_text);
                            String filename = null;
                            if (editText != null) {
                                filename = editText.getText() + Constants.FILE_EXTENSION;
                            }
                            saveFile(filename, true);
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
            }
        };

        DrawerMenuItem drawerExport = new DrawerMenuItem(R.drawable.menu_export, R.string.menu_export) {
            @Override
            public void execute() {
                String filename;

                Calendar calendar = Calendar.getInstance();

                long unixTime = System.currentTimeMillis() / 1000;
                unixTime %= 1000000;

                filename = "IMG_"
                        + calendar.get(Calendar.YEAR)
                        + calendar.get(Calendar.MONTH)
                        + calendar.get(Calendar.DAY_OF_MONTH) + "_" + unixTime + ".jpg";

                screenShot(paper, filename);
            }
        };

        DrawerMenuItem drawerAbout = new DrawerMenuItem(R.drawable.menu_about, R.string.menu_about) {
            @Override
            public void execute() {
                Uri uri = Uri.parse(URL_ABOUT);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                startActivity(intent);
            }
        };

        listMenuItem.add(drawerNew);
        listMenuItem.add(drawerOpen);
        listMenuItem.add(drawerSave);
        listMenuItem.add(drawerExport);
        listMenuItem.add(drawerAbout);
    }

    private void showToast(int p, int lengthLong) {
        Toast toast = Toast.makeText(MainActivity.this, p, lengthLong);
        toast.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle item selection

        switch (item.getItemId()) {
            case R.id.menu_fill:
                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle(getString(R.string.alert_dialog_title_fill));
                alertDialog.setMessage(getString(R.string.alert_dialog_message_fill));
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                        (dialog, which) -> {
                            dialog.dismiss();
                            fillScreen(currentColor);
                            updateDrawerHeader();
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();

                return true;
            case R.id.menu_grid:
                pixelGrid();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean haveTempFile() {
        File imageFolder = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File openFile = new File(imageFolder, Constants.FILE_TEMP_EXTENSION);

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(openFile));
            String value;

            int widthTemp, heightTemp;

            if ((value = bufferedReader.readLine()) != null) {
                widthTemp = Integer.valueOf(value);
            }
            else {
                throw new IOException();
            }

            if ((value = bufferedReader.readLine()) != null) {
                heightTemp = Integer.valueOf(value);
            }
            else {
                throw new IOException();
            }

            return widthTemp > 0 && heightTemp > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void openFile(String fileName, boolean showToast) {
        File imageFolder = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File openFile = new File(imageFolder, fileName);

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(openFile));
            int color;
            String value;

            int widthTemp, heightTemp;

            if ((value = bufferedReader.readLine()) != null) {
                widthTemp = Integer.valueOf(value);
            }
            else {
                throw new IOException();
            }

            if ((value = bufferedReader.readLine()) != null) {
                heightTemp = Integer.valueOf(value);
            }
            else {
                throw new IOException();
            }

            width = widthTemp;
            height = heightTemp;

            initPixelGird();

            LinearLayout linearLayout = findViewById(R.id.paper_linear_layout);

//            Log.i(TAG, "openFile: --widthTemp="+widthTemp+" --heightTemp="+heightTemp);
            for (int i = 0; i < heightTemp; i++) {
                for (int j = 0; j < widthTemp; j++) {

                    if ((value = bufferedReader.readLine()) != null) {
                        color = Integer.valueOf(value);
                    }
                    else {
                        throw new IOException();
                    }
//                    Log.i(TAG, "openFile: --i="+i+" --j="+j);
//                    View v = ((LinearLayout) linearLayout.getChildAt(i)).getChildAt(j);
//                    v.setBackgroundColor(color);
                    ((LinearLayout) linearLayout.getChildAt(i)).getChildAt(j).setBackgroundColor(color);
                }
            }

            showSizeBroad();

            if (showToast) {
                showToast(R.string.file_opened, Toast.LENGTH_SHORT);
            }

        } catch (FileNotFoundException e) {
            Log.e("MainActivity.openFile", "File not found");
            if (showToast) {
                showToast(R.string.file_not_found, Toast.LENGTH_LONG);
            }
        } catch (IOException e) {
            Log.e("MainActivity.openFile", "Could not open file");
            if (showToast) {
                showToast(R.string.file_could_not_open, Toast.LENGTH_LONG);
            }
        }
    }

    public void saveFile(String fileName, boolean showToast) {
        if (isExternalStorageWritable()) {
            Log.e(MainActivity.class.getName(), "External Storage is not writable");
        }

        File imageFolder = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File saveFile = new File(imageFolder, fileName);

        
        try {
            if (!saveFile.exists()) {
                saveFile.createNewFile();
            }

//            Log.i(TAG, "saveFile: w="+width+" h="+height);
            FileWriter fileWriter = new FileWriter(saveFile);
            fileWriter.append(String.valueOf(width))
                    .append("\n")
                    .append(String.valueOf(height))
                    .append("\n");
//            fileWriter.append("16\n16\n");

            LinearLayout linearLayout = findViewById(R.id.paper_linear_layout);
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    View v = ((LinearLayout) linearLayout.getChildAt(i)).getChildAt(j);
                    int color = ((ColorDrawable) v.getBackground()).getColor();
                    fileWriter.append(String.valueOf(color));
                    fileWriter.append("\n");
                }
            }
            fileWriter.flush();
            fileWriter.close();

            if (showToast) {
                showToast(R.string.toast_saved, Toast.LENGTH_SHORT);
            }
        } catch (IOException e) {
            Log.e("MainActivity.saveFile", "File not found");

            if (showToast) {
                showToast(R.string.toast_not_saved, Toast.LENGTH_LONG);
            }
        }
    }

    public void screenShot(View view, String filename) {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_REQUEST_WRITE_STORAGE);

            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),
                view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        if (isExternalStorageWritable()) {
            Log.e(MainActivity.class.getName(), "External storage is not writable");
        }

        File imageFolder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));

        boolean success = true;

        if (!imageFolder.exists()) {
            success = imageFolder.mkdirs();
        }

        if (success) {
            File imageFile = new File(imageFolder, filename);

            FileOutputStream outputStream;

            try {

                if (!imageFile.exists()) {
                    imageFile.createNewFile();
                }

                outputStream = new FileOutputStream(imageFile);
                int quality = 100;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                outputStream.flush();
                outputStream.close();

                openScreenshot(imageFile);
            } catch (FileNotFoundException e) {
                Log.e(MainActivity.class.getName(), "File not found");
            } catch (IOException e) {
                Log.e(MainActivity.class.getName(), "IOException related to generating bitmap file");
            }
        }
        else {
            showToast(R.string.toast_could_not_create_app_folder, Toast.LENGTH_LONG);
        }
    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);

        //Uri uri = Uri.fromFile(imageFile);
        try {
            Uri uri = convertFileToContentUri(this, imageFile);
            intent.setDataAndType(uri, "image/*");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    // can run additional stuff here
                    Toast.makeText(this, R.string.write_permission_granted, Toast.LENGTH_LONG).show();
                }
                else {
                    // permission denied
                    Toast.makeText(this, R.string.write_permission_unavailable, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return !Environment.MEDIA_MOUNTED.equals(state);
    }

    //update preview picture in LeftMenu
    private void updateDrawerHeader() {
//        View paper = findViewById(R.id.paper_linear_layout);
        Bitmap bitmap = Bitmap.createBitmap(paper.getWidth(),
                paper.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        paper.draw(canvas);

        ImageView header = findViewById(R.id.drawer_header);
        header.setImageBitmap(bitmap);
    }

    private void initPalette() {
        arrColor = new ArrayList<>();

        // check have add new color
        String colorsSave = settings.getString(SETTINGS_COLORS, "");
        if (!TextUtils.isEmpty(colorsSave)) {
            String[] arrTemp = colorsSave.split(SPLIT_REGEX);
            for (String item : arrTemp) {
                arrColor.add(Integer.parseInt(item));
            }
        }
        else {
            arrColor.add(ContextCompat.getColor(this, R.color.black));
            arrColor.add(ContextCompat.getColor(this, R.color.eclipse));
            arrColor.add(ContextCompat.getColor(this, R.color.grey));
            arrColor.add(ContextCompat.getColor(this, R.color.silver));
            arrColor.add(ContextCompat.getColor(this, R.color.white));
            arrColor.add(ContextCompat.getColor(this, R.color.red));
            arrColor.add(ContextCompat.getColor(this, R.color.vermilion));
            arrColor.add(ContextCompat.getColor(this, R.color.orange));
            arrColor.add(ContextCompat.getColor(this, R.color.amber));
            arrColor.add(ContextCompat.getColor(this, R.color.yellow));
            arrColor.add(ContextCompat.getColor(this, R.color.lime));
            arrColor.add(ContextCompat.getColor(this, R.color.chartreuse));
            arrColor.add(ContextCompat.getColor(this, R.color.harlequin));
            arrColor.add(ContextCompat.getColor(this, R.color.green));
            arrColor.add(ContextCompat.getColor(this, R.color.malachite));
            arrColor.add(ContextCompat.getColor(this, R.color.mint));
            arrColor.add(ContextCompat.getColor(this, R.color.turquoise));
            arrColor.add(ContextCompat.getColor(this, R.color.cyan));
            arrColor.add(ContextCompat.getColor(this, R.color.sky_blue));
            arrColor.add(ContextCompat.getColor(this, R.color.azure));
            arrColor.add(ContextCompat.getColor(this, R.color.sapphire));
            arrColor.add(ContextCompat.getColor(this, R.color.blue));
            arrColor.add(ContextCompat.getColor(this, R.color.indigo));
            arrColor.add(ContextCompat.getColor(this, R.color.purple));
            arrColor.add(ContextCompat.getColor(this, R.color.lt_purple));
            arrColor.add(ContextCompat.getColor(this, R.color.magenta));
            arrColor.add(ContextCompat.getColor(this, R.color.fuchsia));
            arrColor.add(ContextCompat.getColor(this, R.color.rose));
            arrColor.add(ContextCompat.getColor(this, R.color.carmine));
        }

        

        colorBoxAdapter = new ColorBoxAdapter(arrColor, (position, obj) -> selectColor(obj));
        rcvColor.setAdapter(colorBoxAdapter);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2, RecyclerView.HORIZONTAL, false);
        rcvColor.setLayoutManager(gridLayoutManager);

        selectColor(arrColor.get(0));
    }

    @OnClick({R.id.view_zoom_in})
    protected void onClickZoomIn() {
        if (zoomUtils.currZoom >= ZOOM_MAX) {
            showToast(R.string.toast_zoom_in_max, Toast.LENGTH_SHORT);
        }
        else {
            zoomUtils.zoomByDirection(paper, 1);
        }
    }

    @OnClick({R.id.view_zoom_out})
    protected void onClickZoomOut() {
        if (zoomUtils.currZoom <= ZOOM_MIN) {
            showToast(R.string.toast_zoom_out_min, Toast.LENGTH_SHORT);
        }
        else {
            zoomUtils.zoomByDirection(paper, -1);
        }
    }

    @OnClick({R.id.v_add_color})
    protected void onClickAddColor() {
        Intent i1 = new Intent(MainActivity.this, ColorSelector.class);
        startActivityForResult(i1, RC_ADD_COLOR);
    }

    @OnClick({R.id.new_paper})
    protected void onClickNewPaper() {
        showDialogNewPaper();
    }

    private void showDialogNewPaper() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater layoutInflater = LayoutInflater.from(this);

        builder.setTitle(getString(R.string.new_paper));
        View dialogView = layoutInflater.inflate(R.layout.dialog_new_paper, null);
        builder.setView(dialogView);


        builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());

        EditText edtWidth = dialogView.findViewById(R.id.edt_width);
        EditText edtHeight = dialogView.findViewById(R.id.edt_height);
        if (edtWidth != null) {
            edtWidth.setText(String.valueOf(width));
        }
        if (edtHeight != null) {
            edtHeight.setText(String.valueOf(height));
        }
        builder.setPositiveButton(getString(android.R.string.ok),
                (dialog, which) -> {
                });
        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false);
        alertDialog.setOnShowListener(dialogInterface -> {
            Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(view -> {
                boolean isOk = true;
                String widthTemp = edtWidth.getText().toString();
                String heightTemp = edtHeight.getText().toString();
                if (TextUtils.isEmpty(widthTemp) || widthTemp.equals("0") || !TextUtils.isDigitsOnly(widthTemp)) {
                    edtWidth.setError(getString(R.string.wrong_input_width));
                    isOk = false;
                }
                if (TextUtils.isEmpty(heightTemp) || heightTemp.equals("0") || !TextUtils.isDigitsOnly(heightTemp)) {
                    edtHeight.setError(getString(R.string.wrong_input_height));
                    isOk = false;
                }
                if (isOk) {
                    width = Integer.parseInt(widthTemp);
                    height= Integer.parseInt(heightTemp);
                    grid = false;
                    paper.removeAllViews();
                    initPixelGird();
                    showSizeBroad();
                    alertDialog.dismiss();
                }
            });
        });
        alertDialog.show();
    }

    private void showSizeBroad() {
        tvSizeBroad.setText(getString(R.string.size_broad, width, height));
    }

    ZoomUtils zoomUtils;
    int width = 3;
    int height = 3;

    private void initPixelGird() {

        int x;
        if (grid) {
            x = 0;
        }
        else {
            x = 1;
        }

        zoomUtils.currZoom = getResources().getDimensionPixelSize(R.dimen.pixel_width);
        
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < height; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < width; j++) {
                row.addView(initViewPixel(inflater, x));
            }
            paper.addView(row);
        }
    }

    private TextView initViewPixel(LayoutInflater inflater, int margin) {
        TextView pixel = (TextView) inflater.inflate(R.layout.view_pixel, null);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.pixel_width), getResources().getDimensionPixelSize(R.dimen.pixel_width));
        layoutParams.setMargins(margin, margin, 0, 0);
        pixel.setLayoutParams(layoutParams);

        pixel.setBackgroundColor(ContextCompat.getColor(this, R.color.white));


        //Sets OnLongCLickListener to be able to select current color based on that pixel's color
        pixel.setOnLongClickListener(view -> {
            selectColor(((ColorDrawable) view.getBackground()).getColor());
            return false;
        });
        return pixel;
    }

    //Shows or hides the pixels boundaries from the paper_linear_layout
    private void pixelGrid() {
        int x;
        int y;

        if (grid) {
            x = 0;
            y = 0;
        }
        else {
            x = 1;
            y = 1;
        }

        grid = !grid;

        for (int i = 0; i < paper.getChildCount(); i++) {
            LinearLayout l = (LinearLayout) paper.getChildAt(i);

            for (int j = 0; j < l.getChildCount(); j++) {
                View pixel = l.getChildAt(j);

                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) pixel.getLayoutParams();

                layoutParams.setMargins(x, y, 0, 0);
                pixel.setLayoutParams(layoutParams);
            }
        }
    }

    //Fills paper_linear_layout with chosen color
    private void fillScreen(int color) {
        for (int i = 0; i < paper.getChildCount(); i++) {
            LinearLayout l = (LinearLayout) paper.getChildAt(i);

            for (int j = 0; j < l.getChildCount(); j++) {
                View pixel = l.getChildAt(j);

                pixel.setBackgroundColor(color);
            }
        }
    }

    //Sets the current color based on the "color" argument
    public void selectColor(int color) {
        currentColor = color;

        vCurrColorSelected.setBackgroundColor(currentColor);
    }

    public void saveColors() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrColor.size(); i++) {
            sb.append(arrColor.get(i)).append(i==arrColor.size()-1?"":SPLIT_REGEX);
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SETTINGS_COLORS, sb.toString());
        editor.apply();
    }

    //Onclick method that changes the color of a single "pixel"
    public void changeColor(View v) {
        v.setBackgroundColor(currentColor);
    }
}

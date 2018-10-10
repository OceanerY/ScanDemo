package com.yangs.scandemo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.activity.CaptureActivity;
import com.yangs.scandemo.util.Constant;

import static com.blankj.utilcode.util.FileIOUtils.readFile2String;
import static com.blankj.utilcode.util.FileIOUtils.writeFileFromString;
import static com.blankj.utilcode.util.FileUtils.isFileExists;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private Button mScan;
    private TextView mMsg;
    private EditText printcontent;
    private Button bt_print;
    private Button bt_Saveprint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化
        InitView();
        InitEvent();
        //绑定服务
        bindService();
        if (mIzkcService != null) {
            Toast.makeText(this, "服务绑定成功！", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "服务绑定失败，禁止用打印功能！", Toast.LENGTH_SHORT).show();
            bt_print.setEnabled(false);
        }
        if (isFileExists(this.getFilesDir() + "/saveprint.doc")) {
            String ms = readFile2String(this.getFilesDir() + "/saveprint.doc");
            printcontent.setText(ms);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }

    private void InitEvent() {
        mScan.setOnClickListener(this);
        bt_print.setOnClickListener(this);
        bt_Saveprint.setOnClickListener(this);
    }

    private void InitView() {
        mScan = (Button) findViewById(R.id.bt_scan);
        bt_print = (Button) findViewById(R.id.bt_print);
        bt_Saveprint = (Button) findViewById(R.id.bt_Saveprint);
        mMsg = (TextView) findViewById(R.id.txtView);
        printcontent = (EditText) findViewById(R.id.et_printcontent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_scan:
                try {
                    startQrCode();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bt_print:
                try {
                    mIzkcService.setModuleFlag(0);
                    mIzkcService.printerInit();
                    //获取打印机状态
                    String status = mIzkcService.getPrinterStatus();
                    //获取打印机版本
                    String aidlServiceVersion = mIzkcService.getServiceVersion();
                    mMsg.setText("打印机状态：" + status + "\n" + "打印机版本信息：" + aidlServiceVersion);

                    String msg = printcontent.getText().toString().trim();

                    if ("".equals(msg)) {
                        Toast.makeText(MainActivity.this, "打印的内容为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        //判断打印机是否空闲
                        if (mIzkcService.checkPrinterAvailable()) {
                            mIzkcService.printGBKText(msg);
                        } else {
                            Toast.makeText(this, "打印机正忙，不可用", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bt_Saveprint:
                String msg = printcontent.getText().toString().trim();
                writeFileFromString(this.getFilesDir() + "/saveprint.doc", msg);
                printcontent.setText(msg);
                break;
            default:
                break;
        }

    }

    private void startQrCode() {
        performCodeWithPermission("摄像头权限申请", new PermissionCallback() {
            @Override
            public void hasPermission() {
                // 二维码扫码
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivityForResult(intent, Constant.REQ_QR_CODE);
            }

            @Override
            public void noPermission() {
                Toast.makeText(MainActivity.this, "请至权限中心打开本应用的相机访问权限", Toast.LENGTH_LONG).show();
            }
        }, Manifest.permission.CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //扫描结果回调
        if (requestCode == Constant.REQ_QR_CODE && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString(Constant.INTENT_EXTRA_KEY_QR_SCAN);
            //将扫描出的信息显示出来
            mMsg.setText(scanResult);
        }
    }
}

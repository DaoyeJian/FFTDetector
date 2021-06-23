//FFT Detector version 2.0.1
//2021.6.20 更新

package com.motofamdmn.you.fftdetector

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.tabs.TabLayout
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    private val LOG_TAG = "AudioRecordTest"

    private lateinit var realm: Realm

    private val REQUEST_PERMISSION_ID = 200

    private var permissionToRecordAccepted = false  //録音パーミッションフラグ
    private var permissionToWriteExternalStorageAccepted = false  //外部ストレージ書込みパーミッションフラグ

    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE)

    // 録音と外部ストレージ書込みのパーミッション確認
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION_ID) {
            // 識別IDでリクエストを判断
            if (grantResults.isNotEmpty()) {
                // 処理された
                for (i in permissions.indices) {
                    // 複数リクエストがあった場合
                    if (permissions[i] == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        // 外部ストレージのパーミッション
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            //許可
                            permissionToWriteExternalStorageAccepted = true
                        } else {
                            // 拒否
                        }
                    }
                    if (permissions[i] == Manifest.permission.RECORD_AUDIO) {
                        // 録音のパーミッション
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            //許可
                            permissionToRecordAccepted = true
                        } else {
                            // 拒否
                        }
                    }

                }
            }
        }

        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //permissionToRecordAccepted = if (requestCode == REQUEST_PERMISSION_ID) {
        //    grantResults[0] == PackageManager.PERMISSION_GRANTED
        //} else {
        //    false
        //}
        if (!permissionToRecordAccepted) finish()
    }

    //オプションメニューの表示
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //オプションメニューが選択された時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.termsOfUseItem -> {  //利用規約
                AlertDialog.Builder(this)
                    .setView(R.layout.terms_of_use)
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        // Do nothing
                    }
                    .show()
                true
            }
            R.id.privacyPolicyItem -> {  //プライバシーポリシー
                AlertDialog.Builder(this)
                    .setView(R.layout.oss_license)
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        // Do nothing
                    }
                    .show()
                true
            }
            R.id.versionInfoItem -> {  //バージョン情報
                AlertDialog.Builder(this)
                    .setView(R.layout.version_info)
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        // Do nothing
                    }
                    .show()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Otoha)  //splash画面テーマを元のAPPThemeに戻す
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //splash画面
        //setTheme(R.style.AppTheme_Splash)
        setContentView(R.layout.activity_main)

        //`ツールバー（アクションバー）の設定
        toolbar.setTitle("FFT Detector")
        setSupportActionBar(findViewById(R.id.toolbar))
        //toolbar.setLogo(R.drawable.ic_launcher_foreground)

        // 録音のパーミッションリクエスト
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_ID)

        //realmデータベース
        realm = Realm.getDefaultInstance()
        val files = realm.where<myFiles>().findAll()

        //全フラグメントからアクセス可能の共通データ
        val cd = commonData.getInstance()

        //データ保存フォルダ
        var dirFlag = 0
        val mydirName = "testwave"

        // フォルダーを使用する場合、あるかを確認
        val myDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            mydirName
        )
        if (!myDir.exists()) {
            Log.d("DIR", "フォルダ無し")
            // なければ、フォルダーを作る
            if (myDir.mkdirs()) {
                dirFlag = 1
                Log.d("DIR", "フォルダ作成成功")
            } else {
                dirFlag = 0
                Log.d("DIR", "フォルダ作成失敗")
            }
        }else{
            Log.d("DIR", "フォルダ有り")
        }

        val myFileList = myDir.list()  //myDirフォルダのファイルリスト格納
        var tempFileName : String = ""
        var tempFileSize : Int = 0
        var tempFilePath = ""
        var fileNum = myFileList.size

        if(fileNum != 0) {
            for (i in 0..fileNum - 1) {
                tempFilePath = myDir.getPath() + "/" + myFileList[i]
                val tempFile = File(tempFilePath)
                val tempWav = myWavRead()
                tempWav.read(myFileList[i])

                realm.executeTransaction { db: Realm ->
                    val maxId = db.where<myFiles>().max("id")
                    val nextId = (maxId?.toLong() ?: 0L) + 1
                    val myFile = db.createObject<myFiles>(nextId)
                    myFile.fileName = myFileList[i]
                    myFile.fileSize = tempFile.length()
                    myFile.stereoMonoral = cd.stereoMonoral
                    myFile.sampleRate = cd.sampleRate
                    myFile.dataBit = cd.dataBits
                }

            }
        }

        val fragmentManager = this.getSupportFragmentManager()

        //録音フラグメント（recordSound）の生成,tagをrecordFragmentとする
        val fragment = recordSound()
        val fragmentTransaction = fragmentManager.beginTransaction().setTransition(
            FragmentTransaction.TRANSIT_FRAGMENT_OPEN
        )
        fragmentTransaction.add(R.id.container, fragment, "recordSoundFragment").commit()

        // TabLayoutの取得
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        var tPosition = 0

        // OnTabSelectedListenerの実装
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            // タブが選択された際に呼ばれる
            override fun onTabSelected(tab: TabLayout.Tab) {

                val tabPosition = tab.position
                val nowFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.container)
                val rSFg = fragmentManager.findFragmentByTag("recordSoundFragment")
                val fAFg = fragmentManager.findFragmentByTag("fftAnalisysFragment")
                val sCFg = fragmentManager.findFragmentByTag("setConditionFragment")
                val fOFg = fragmentManager.findFragmentByTag("fileOpenFragment")

                when (tabPosition) {
                    0 -> {
                        val rFragment = recordSound()
                        val mfragmentTransaction = fragmentManager.beginTransaction().setTransition(
                            FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                        )
                        if (fAFg != null) {
                            mfragmentTransaction.remove(fAFg)
                            mfragmentTransaction.hide(fAFg)
                        }
                        if (sCFg != null && tPosition == 2) {
                            mfragmentTransaction.hide(sCFg)
                        }
                        if (fOFg != null && tPosition == 3) {
                            mfragmentTransaction.hide(fOFg)
                        }
                        if (rSFg != null) {
                            mfragmentTransaction.show(rSFg)
                        } else {
                            mfragmentTransaction.add(
                                R.id.container,
                                rFragment,
                                "recordSoundFragment"
                            )
                        }
                        mfragmentTransaction.commit()
                        tPosition = 0
                    }

                    1 -> {
                        val fFragment = fftAnalisys()
                        val mfragmentTransaction = fragmentManager.beginTransaction().setTransition(
                            FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                        )

                        if (rSFg != null && tPosition == 0) {
                            mfragmentTransaction.hide(rSFg)
                        }
                        if (sCFg != null && tPosition == 2) {
                            mfragmentTransaction.hide(sCFg)
                        }
                        if (fOFg != null && tPosition == 3) {
                            mfragmentTransaction.hide(fOFg)
                        }
                        if (fAFg != null) {
                            mfragmentTransaction.remove(fAFg)
                        } else {
                            mfragmentTransaction.add(
                                R.id.container,
                                fFragment,
                                "fftAnalisysFragment"
                            )
                        }
                        mfragmentTransaction.commit()
                        tPosition = 1

                    }

                    2 -> {
                        val sFragment = setCondition()
                        val mfragmentTransaction = fragmentManager.beginTransaction().setTransition(
                            FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                        )

                        if (rSFg != null && tPosition == 0) {
                            mfragmentTransaction.hide(rSFg)
                        }
                        if (fAFg != null) {
                            mfragmentTransaction.remove(fAFg)
                            //mfragmentTransaction.hide(fAFg)
                        }
                        if (fOFg != null && tPosition == 3) {
                            mfragmentTransaction.hide(fOFg)
                        }
                        if (sCFg != null) {
                            mfragmentTransaction.show(sCFg)
                        } else {
                            mfragmentTransaction.add(
                                R.id.container,
                                sFragment,
                                "setConditionFragment"
                            )
                        }
                        mfragmentTransaction.commit()
                        tPosition = 2

                    }

                    else -> {
                        val fOFragment = fileOpenFragment()
                        val mfragmentTransaction = fragmentManager.beginTransaction().setTransition(
                            FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                        )

                        if (rSFg != null && tPosition == 0) {
                            mfragmentTransaction.hide(rSFg)
                        }
                        if (fAFg != null) {
                            mfragmentTransaction.remove(fAFg)
                            //mfragmentTransaction.hide(fAFg)
                        }
                        if (sCFg != null && tPosition == 2) {
                            mfragmentTransaction.hide(sCFg)
                        }
                        if (fOFg != null) {
                            mfragmentTransaction.show(fOFg)
                        }else {
                            mfragmentTransaction.add(
                                R.id.container,
                                fOFragment,
                                "fileOpenFragment"
                            )
                        }
                        mfragmentTransaction.commit()
                        tPosition = 3

                    }

                }

            }

            // タブが未選択になった際に呼ばれる
            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            // 同じタブが選択された際に呼ばれる
            override fun onTabReselected(tab: TabLayout.Tab) {

            }


        })

    }


}
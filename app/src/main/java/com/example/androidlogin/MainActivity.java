package com.example.androidlogin;  // 패키지명: 현재 앱의 기본 패키지 (폴더 경로와 비슷한 개념)

//*import: 다른 패키지에 있는 class를 데려옴
import android.content.Intent;  // 화면 전환을 위한 Intent 클래스
import android.os.Bundle;  // 액티비티의 상태를 저장하고 복원하는 데 사용됨
import android.util.Log;   // 로그를 출력하여 디버깅하는 데 사용됨
import android.util.Patterns;   // 이메일 형식을 검증하는 데 사용됨
import android.view.View;     // UI 요소를 클릭할 때 사용됨
import android.widget.Button;     // 버튼 UI 요소를 사용하기 위한 클래스
import android.widget.EditText;      // 이미지가 포함된 버튼
import android.widget.ImageButton;     
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.espresso.remote.EspressoRemoteMessage;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    //*Pattern 클래스는 정규 표현식(Regex, Regular Expression) 을 다룰 때 사용하는 Java 내장 클래스임(특정한 문자열이 정해진 패턴(규칙)에 맞는지 검사)
    // 비밀번호 정규식 (4~16자의 영문,숫자,특수문자 허용)
    Pattern PASSWORD_PATTERN = Pattern.compile("^[a-zA-Z0-9!@.#$%^&*?_~]{4,16}$");

    //*Firebase에서 제공하는 사용자 인증 클래스이며 사용자의 로그인 정보(이메일, 구글 로그인, 페이스북 로그인 등)를 관리하는 역할을 함.
    // 파이어베이스 인증 객체 생성(로그인,회원가입,로그아웃 등을 처리)
    private FirebaseAuth firebaseAuth;

️    //* FirebaseAuth.getInstance() → Firebase의 로그인 관리 시스템을 가져옴.
    //*.getCurrentUser() → 현재 로그인한 사용자가 있는지 확인하고 정보를 가져옴.️ 
    //* FirebaseUser user → 사용자 정보를 user 변수에 저장
    // 현재 로그인한 Firebase 사용자 정보를 저장하는 변수
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    // 사용자가 입력한 이메일과 비밀번호 입력칸
    private EditText editTextEmail;
    private EditText editTextPassword;

    // 사용자가 입력한 이메일과 비밀번호를 저장할 변수
    private String email = "";
    private String password = "";

    // 구글 로그인 객체 생성->Google 로그인 클라이언트 객체
    //*GoogleSignInClient 사용자가 Google 계정으로 로그인할 수 있게 도와주는 기능을 제공
    //*mGoogleSignInClient는 GoogleSignInClient 객체를 저장할 변수
    //*RC_SIGN_IN은 Google 로그인 요청을 구분하는 상수 값으로 사용됨, 9001은 로그인 요청 코드
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    // 페이스북 로그인 객체 생성
    private LoginButton btn_facebook_login;
    private CallbackManager mCallbackManager;

    @Override
    public void onBackPressed() {
        // 뒤로 가기 버튼을 누르면 메인화면(MenuActivity)로 이동(현재 활동 종료->MenuActivity로 이동하려고 함)
        Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();  // 현재 액티비티 종료
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   // XML 레이아웃 설정

        // Firebase 인증 객체 초기화
        firebaseAuth = FirebaseAuth.getInstance();

        //* XML에서 이메일,비밀번호 입력칸 가져오기
        // id가 write_email인 editText에 대한 메서드 저장
        editTextEmail = findViewById(R.id.et_eamil);
        // id가 signup_password인 editText에 대한 메서드 저장
        editTextPassword = findViewById(R.id.et_password);

        // 회원가입 버튼 객체 생성
        Button signup_btn = findViewById(R.id.btn_signUp);
        // 비밀번호 재설정 버튼 객체 생성
        Button findpw_btn = findViewById(R.id.btn_findpw);
        // 이메일 찾기 버튼 객체 생성
        Button findid_btn = findViewById(R.id.btn_findid);

        // 페이스북 로그인 버튼 생성
        mCallbackManager = CallbackManager.Factory.create();

        // Facebook 로그인 버튼을 XML에서 찾아서 Java 객체로 가져옴
        btn_facebook_login = (LoginButton) findViewById(R.id.facebook_login_button);
        // 사용자에게 요청할 권한을 설정(이메일과 공개 프로필 정보)
        btn_facebook_login.setReadPermissions(Arrays.asList("public_profile", "email"));
        // Facebook 로그인 버튼에 대한 콜백 함수를 등록
        btn_facebook_login.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            //로그인 성공 시 호출되는 메서드
            @Override
            public void onSuccess(LoginResult loginResult) {
                //로그인 성공 메시지를 로그에 출력
                Log.e("페이스북 로그인", "facebook:onSuccess:" + loginResult);

                //로그인 성공 후 Facebook의 엑세스 토큰을 이용하여 Firebase 인증 처리
                handleFacebookAccessToken(loginResult.getAccessToken());
            }
            //사용자가 로그인 취소했을때 호출되는 메서드
            @Override
            public void onCancel() {
                //로그인 취소 메시지를 로그에 출력
                Log.e("페이스북 로그인", "facebook:onCancel");
            }
            //로그인 중 오류가 발생했을 때 호출되는 메서드
            @Override
            public void onError(FacebookException error) {
                // 오류 메시지를 로그에 출력력
                Log.d("페이스북 로그인", "facebook:onError", error);
            }
        });

        // 구글 로그인 버튼 객체 생성
        SignInButton signInButton = findViewById(R.id.signInButton);

        // 홈으로 이동하는 버튼 객체 생성
        ImageButton btn_home = findViewById(R.id.gohome);

        // 홈 버튼 onclicklistener 생성
        btn_home.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // 버튼을 누르면 메인화면으로 이동
                Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

        });

        // 회원가입 버튼 onclicklistener 생성
        signup_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // 회원가입 버튼을 누르면 회원가입 레이아웃으로 이동
                Intent intent = new Intent(getApplicationContext(),SignupActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // 비밀번호 재설정 버튼 onclicklistener 생성
        findpw_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 비밀번호 재설정 버튼을 누르면 비밀번호 찾기 레이아웃으로 이동
                Intent intent = new Intent(getApplicationContext(),FindpwActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // 이메일 찾기 버튼 onclicklistener 생성
        findid_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // 이메일 찾기 버튼을 누르면 이메일 찾기 레이아웃으로 이동
                Intent intent = new Intent(getApplicationContext(), FindIdActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // 구글 로그인
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 구글 로그인 버튼  onClicklistener 생성
        signInButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Log.e("구글 로그인","버튼 클릭");
                // 클릭시 구글 로그인 메서드 실행
                signIn();

            }
        });
    }

    // 구글 로그인 메서드
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
        Log.e("구글 로그인","메서드 실행");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("구글 로그인","result 메서드");
        // RC_SIGN_IN을 통해 로그인 확인여부 코드가 저상 전달되었다면
        if (requestCode == RC_SIGN_IN) {
            Log.e("구글 로그인","로그인 여부");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // 구글 로그인이 성공하면, 파이어베이스에 로그인 인증 등록
                GoogleSignInAccount account = task.getResult(ApiException.class);
                // 구글 이용자가 확인된 사용자 정보를 파이어베이스로 넘기기
                firebaseAuthWithGoogle(account);
                Log.e("구글 로그인","구글 로그인 성공");
            } catch (ApiException ignored) {
                Log.e("구글 로그인","구글 로그인 실패");
            }
        }
    }

    // 파이어베이스와 구글 로그인 연결
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        //디버깅을 위해 로그 출력(구글 로그인 시작)
        Log.e("구글 로그인","파이어베이스랑 연결 중");
        // 파이어베이스로 받은 구글 사용자가 확인된 이용자의 값을 토큰으로 받음
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        //Firebase 인증 객체(firebaseAuth)를 사용하여 구글 로그인 정보를 Firebase에 넘겨줌
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //로그인 성공 여부 확인
                        if (task.isSuccessful()) {
                            // 로그인에 성공하면 "로그인 성공" 토스트를 보여줌
                            Toast.makeText(MainActivity.this, R.string.success_login, Toast.LENGTH_SHORT).show();
                            //현재 로그인한 사용자 정보를 가져옴
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            //로그인 성공 후 MenuActivity로 이동하기 위한 Intent 생성
                            Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                            //새로운 화면을 띄울때 기존 액티비티를 정리하고 새 직업(Task)로 시작
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            //MenuActivity 실행(로그인 후 이동)
                            startActivity(intent);
                            //현재 액티비티(MainActivity) 종료 (뒤로 가기로 돌아올 수 없게 만듦)
                            finish();
                        } else {
                            // 로그인에 실패하면 "로그인 실패" 토스트를 보여줌
                            Toast.makeText(MainActivity.this, R.string.failed_login, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // 파이어베이스와 페이스북 로그인 연결
    private void handleFacebookAccessToken(AccessToken token) {
        Log.e("페이스북 로그인", "handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                           // 로그인 성공시
                            Toast.makeText(MainActivity.this, R.string.success_login, Toast.LENGTH_SHORT).show();
                            Log.e("페이스북 로그인", "signInWithCredential:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            // 로그인에 실패하면 "로그인 실패" 토스트를 보여줌
                            Toast.makeText(MainActivity.this, R.string.failed_login, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    // 이메일 로그인 메서드
    public void signInemail(View view) {

        // editText에 작성한 내용을 String으로 변환하여 객체에 저장
        email = editTextEmail.getText().toString();
        password = editTextPassword.getText().toString();

        // 유효성 검사 후 로그인 메서드 실행
        if(isValidEmail() && isValidPasswd()) {
            loginUser(email, password);
        }
    }

    // 이메일 유효성 검사
    private boolean isValidEmail() {
        if (email.isEmpty()) {
            // 이메일 칸이 공백이면 false
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // 이메일 형식이 불일치하면 false
            return false;
        } else {
            return true;
        }
    }

    // 비밀번호 유효성 검사
    private boolean isValidPasswd() {
        if (password.isEmpty()) {
            // 비밀번호 칸이 공백이면 fasle
            return false;
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            // 비밀번호 형식이 불일치하면 false
            return false;
        } else {
            return true;
        }
    }

    // 로그인 메서드
    private void loginUser(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            if(firebaseAuth.getCurrentUser().isEmailVerified()){
                                // 로그인에 성공하면 "로그인 성공" 토스트를 보여줌
                                Toast.makeText(MainActivity.this, R.string.success_login, Toast.LENGTH_SHORT).show();
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                // MenuActivity로 화면 전환
                                Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                            else{
                                Toast.makeText(MainActivity.this,"이메일 인증을 완료해주세요.",Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // 로그인에 실패하면 "로그인 실패" 토스트를 보여줌
                            Toast.makeText(MainActivity.this, R.string.failed_login, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

}

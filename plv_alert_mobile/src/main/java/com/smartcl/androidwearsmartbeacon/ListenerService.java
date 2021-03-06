package com.smartcl.androidwearsmartbeacon;

import android.content.Intent;
import android.net.Uri;

import com.android.volley.VolleyError;
import com.google.android.gms.wearable.MessageEvent;
import com.smartcl.communicationlibrary.BaseListenerService;
import com.smartcl.communicationlibrary.NetworkAnswer;
import com.smartcl.communicationlibrary.NetworkOperation;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Listener which reacts to the messages sent by the wearable device.
 * These messages can mean we detected a smartbeacon, or we got an answer from a question.
 * <p/>
 * When a smartbeacon is found, a question is sent to the wearable device.
 * The answer is saved within the database on the server side..
 */
public class ListenerService extends BaseListenerService {

    public static final String BEACON_ENTERED_PATH = "/beacon/entered/";
    public static final String QUESTION_ANSWER_PATH = "/question/answer/";
    public static final String APP_OPEN_PATH = "/app/open/lcl/";
    public static final String QUESTION_QUESTION_PATH = "/question/question";
    public static final String GET_PREFERENCES_PATH = "/preferences/";
    public static final String SEND_PREFERENCES_PATH = "/preferences/data/";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String path = messageEvent.getPath();
        switch (path) {
            case BEACON_ENTERED_PATH:
                beaconHasEntered(messageEvent);
                break;
            case QUESTION_ANSWER_PATH:
                getAnswer(messageEvent);
                break;
            case APP_OPEN_PATH:
                openApp(messageEvent);
                break;
            case GET_PREFERENCES_PATH:
                getPreferences(messageEvent);
                break;
            default:
                //showToast("Unknown message:" + path);
                break;
        }
    }

    private void getAnswer(MessageEvent messageEvent) {
        JSONObject json = extractJsonFromMessage(messageEvent);
        final String answer = (String) json.get("answer");
        final String question = (String) json.get("question");
        final String username = getCommonSharedPreferences().getString("name", "");
        final String serverUrl = getCommonSharedPreferences().getString("api_url", "");

        NetworkOperation network = new NetworkOperation(this, serverUrl);
        NetworkAnswerGetAnswer networkAnswerGetAnswer = new NetworkAnswerGetAnswer(network);
        network.operationGet(networkAnswerGetAnswer.getAnswerUrl(question, username, answer),
                             networkAnswerGetAnswer);
    }

    private void beaconHasEntered(MessageEvent messageEvent) {
        JSONObject json = extractJsonFromMessage(messageEvent);
        final long major = (long) json.get("major");
        final long minor = (long) json.get("minor");
        final String username = getCommonSharedPreferences().getString("name", "");
        final String serverUrl = getCommonSharedPreferences().getString("api_url", "");

        NetworkOperation network = new NetworkOperation(this, serverUrl);
        NetworkAnswerBeaconInfo networkAnswerBeaconInfo = new NetworkAnswerBeaconInfo(network,
                                                                                      username);
        network.operationGet(networkAnswerBeaconInfo.getBeaconProfileUrl(String.valueOf(major),
                                                                         String.valueOf(minor)),
                             networkAnswerBeaconInfo);
    }

    private void openApp(MessageEvent messageEvent) {
        JSONObject json = extractJsonFromMessage(messageEvent);

        final String appPackageName = (String) json.get("details");
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                                       Uri.parse("market://details?id=" + appPackageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException anfe) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://play.google.com/store/apps/details?id=" + appPackageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void getPreferences(MessageEvent messageEvent) {
        byte[] preferencesSerialized = getPreferencesSerialized(messageEvent);
        if (preferencesSerialized != null) {
            _messageSender.sendMessage(SEND_PREFERENCES_PATH, preferencesSerialized);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    ////// Network Operations
    //////////////////////////////////////////////////////////////////////////////////
    class NetworkAnswerBeaconInfo extends NetworkAnswer {

        private String _username;

        public NetworkAnswerBeaconInfo(NetworkOperation network, String username) {
            super(network);
            _username = username;
        }

        public String getBeaconProfileUrl(String major, String minor) {
            return _network.getApiUrl() + "beacon/get?major=" + major + "&minor=" + minor;
        }

        @Override
        public void run(Object response) {
            JSONObject jsonResponse = (JSONObject) JSONValue.parse((String) response);
            final String profile = (String) jsonResponse.get("profile");
            final String usecase = (String) jsonResponse.get("usecase");

            // If not for this use case.
            if ("PLV_Alert".toLowerCase().compareTo(usecase.toLowerCase()) != 0) {
                //showToast("not a plv alert:" + usecase);
                return;
            }

            NetworkAnswerStudentInfo networkAnswerStudentInfo = new NetworkAnswerStudentInfo(
                    _network, profile);
            _network.operationGet(networkAnswerStudentInfo.getSignInUrl(_username),
                                  networkAnswerStudentInfo);
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            showToast("Error beacon info");
        }
    }

    class NetworkAnswerStudentInfo extends NetworkAnswer {

        private final String _profileExpected;

        public NetworkAnswerStudentInfo(NetworkOperation network, String profileExpected) {
            super(network);
            _profileExpected = profileExpected;
        }

        public String getSignInUrl(String name) {
            return _network.getApiUrl() + "user/signin?name=" + name;
        }

        @Override
        public void run(Object response) {
            JSONObject jsonResponse = (JSONObject) JSONValue.parse((String) response);
            final String status = (String) jsonResponse.get("status");

            // If not the correct profile.
            if (status.compareTo(_profileExpected) != 0) {
                //showToast("not a " + _profileExpected);
                return;
            }

            NetworkAnswerGetQuestion networkAnswerGetQuestion = new NetworkAnswerGetQuestion(
                    _network);
            _network.operationGet(networkAnswerGetQuestion.getQuestionUrl(status),
                                  networkAnswerGetQuestion);
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            showToast("Error student info");
        }
    }

    class NetworkAnswerGetQuestion extends NetworkAnswer {

        public NetworkAnswerGetQuestion(NetworkOperation network) {
            super(network);
        }

        public String getQuestionUrl(String status) {
            return _network.getApiUrl() + "question/ask?status=" + status;
        }

        @Override
        public void onResponse(Object response) {
            JSONObject json = new JSONObject();
            json.put("question", response);
            _messageSender.sendMessage(QUESTION_QUESTION_PATH, json);
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            showToast("Error answer get question");
        }
    }

    class NetworkAnswerGetAnswer extends NetworkAnswer {

        public NetworkAnswerGetAnswer(NetworkOperation network) {
            super(network);
        }

        public String getAnswerUrl(String title, String name, String answer) {
            final String url = _network.getApiUrl() + "question/answer?title=useless" +
                    "&name=" + name +
                    "&answer=" + answer;
            return url;
        }

        @Override
        public void onResponse(Object response) {
            showToast("A mail has been sent");
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            showToast("Error mail to send");
        }
    }
}

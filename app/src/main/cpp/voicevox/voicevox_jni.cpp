#include <jni.h>
#include <string>
#include <android/log.h>
#include "voicevox/voicevox_core.h"

#define LOG_TAG "VoicevoxJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static VoicevoxSynthesizer* synthesizer = nullptr;
static VoicevoxOnnxruntime* onnxruntime = nullptr;
static OpenJtalkRc* openJtalk = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_japaneselearningapp_tts_VoicevoxBridge_initialize(
        JNIEnv* env, jobject thiz, jstring dict_path, jstring onnxruntime_path) {
    const char* dictPath = env->GetStringUTFChars(dict_path, nullptr);
    const char* ortPath = env->GetStringUTFChars(onnxruntime_path, nullptr);

    LOGD("Initializing VoiceVox Core...");
    LOGD("Dict path: %s", dictPath);
    LOGD("ONNX Runtime path: %s", ortPath);

    VoicevoxLoadOnnxruntimeOptions ortOptions = {
            .filename = ortPath
    };

    VoicevoxResultCode ortResult = voicevox_onnxruntime_load_once(ortOptions, &onnxruntime);
    if (ortResult != VOICEVOX_RESULT_OK) {
        LOGE("Failed to load ONNX Runtime: %d", ortResult);
        env->ReleaseStringUTFChars(dict_path, dictPath);
        env->ReleaseStringUTFChars(onnxruntime_path, ortPath);
        return JNI_FALSE;
    }
    LOGD("ONNX Runtime loaded");

    VoicevoxResultCode jtalkResult = voicevox_open_jtalk_rc_new(dictPath, &openJtalk);
    if (jtalkResult != VOICEVOX_RESULT_OK) {
        LOGE("Failed to create OpenJtalk: %d", jtalkResult);
        env->ReleaseStringUTFChars(dict_path, dictPath);
        env->ReleaseStringUTFChars(onnxruntime_path, ortPath);
        return JNI_FALSE;
    }
    LOGD("OpenJtalk created");

    VoicevoxInitializeOptions initOptions = voicevox_make_default_initialize_options();
    initOptions.acceleration_mode = VOICEVOX_ACCELERATION_MODE_AUTO;
    initOptions.cpu_num_threads = 4;

    VoicevoxResultCode synthResult = voicevox_synthesizer_new(onnxruntime, openJtalk, initOptions, &synthesizer);
    if (synthResult != VOICEVOX_RESULT_OK) {
        LOGE("Failed to create synthesizer: %d", synthResult);
        voicevox_open_jtalk_rc_delete(openJtalk);
        openJtalk = nullptr;
        env->ReleaseStringUTFChars(dict_path, dictPath);
        env->ReleaseStringUTFChars(onnxruntime_path, ortPath);
        return JNI_FALSE;
    }

    LOGD("VoiceVox Core initialized successfully");

    env->ReleaseStringUTFChars(dict_path, dictPath);
    env->ReleaseStringUTFChars(onnxruntime_path, ortPath);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_japaneselearningapp_tts_VoicevoxBridge_loadVoiceModel(
        JNIEnv* env, jobject thiz, jstring model_path) {
    if (synthesizer == nullptr) {
        LOGE("Synthesizer not initialized");
        return -1;
    }

    const char* modelPath = env->GetStringUTFChars(model_path, nullptr);
    LOGD("Loading voice model: %s", modelPath);

    VoicevoxVoiceModelFile* modelFile;
    VoicevoxResultCode openResult = voicevox_voice_model_file_open(modelPath, &modelFile);
    if (openResult != VOICEVOX_RESULT_OK) {
        LOGE("Failed to open voice model file: %d", openResult);
        env->ReleaseStringUTFChars(model_path, modelPath);
        return -1;
    }

    VoicevoxResultCode loadResult = voicevox_synthesizer_load_voice_model(synthesizer, modelFile);
    voicevox_voice_model_file_delete(modelFile);

    if (loadResult != VOICEVOX_RESULT_OK) {
        LOGE("Failed to load voice model: %d", loadResult);
        env->ReleaseStringUTFChars(model_path, modelPath);
        return -1;
    }

    char* metasJson = voicevox_synthesizer_create_metas_json(synthesizer);
    if (metasJson != nullptr) {
        LOGD("Voice model metas: %s", metasJson);
        voicevox_json_free(metasJson);
    }

    LOGD("Voice model loaded successfully");
    env->ReleaseStringUTFChars(model_path, modelPath);
    return 0;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_example_japaneselearningapp_tts_VoicevoxBridge_synthesis(
        JNIEnv* env, jobject thiz, jstring text, jint style_id) {
    if (synthesizer == nullptr) {
        LOGE("Synthesizer not initialized");
        return nullptr;
    }

    const char* textStr = env->GetStringUTFChars(text, nullptr);
    LOGD("Synthesizing text: %s, style_id: %d", textStr, style_id);

    char* audioQueryJson;
    VoicevoxResultCode queryResult = voicevox_synthesizer_create_audio_query(synthesizer, textStr, (VoicevoxStyleId)style_id, &audioQueryJson);

    if (queryResult != VOICEVOX_RESULT_OK) {
        LOGE("Failed to create audio query: %d", queryResult);
        env->ReleaseStringUTFChars(text, textStr);
        return nullptr;
    }

    VoicevoxSynthesisOptions synthOptions = voicevox_make_default_synthesis_options();

    uintptr_t wavLength;
    uint8_t* wavData;
    VoicevoxResultCode synthResult = voicevox_synthesizer_synthesis(synthesizer, audioQueryJson, (VoicevoxStyleId)style_id, synthOptions, &wavLength, &wavData);

    voicevox_json_free(audioQueryJson);

    if (synthResult != VOICEVOX_RESULT_OK) {
        LOGE("Failed to synthesize: %d", synthResult);
        env->ReleaseStringUTFChars(text, textStr);
        return nullptr;
    }

    LOGD("Synthesis completed, wav length: %zu", (size_t)wavLength);

    jbyteArray wavArray = env->NewByteArray((jsize)wavLength);
    env->SetByteArrayRegion(wavArray, 0, (jsize)wavLength, (const jbyte*)wavData);

    voicevox_wav_free(wavData);
    env->ReleaseStringUTFChars(text, textStr);

    return wavArray;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_japaneselearningapp_tts_VoicevoxBridge_release(
        JNIEnv* env, jobject thiz) {
    LOGD("Releasing VoiceVox Core...");

    if (synthesizer != nullptr) {
        voicevox_synthesizer_delete(synthesizer);
        synthesizer = nullptr;
    }

    if (openJtalk != nullptr) {
        voicevox_open_jtalk_rc_delete(openJtalk);
        openJtalk = nullptr;
    }

    LOGD("VoiceVox Core released");
}
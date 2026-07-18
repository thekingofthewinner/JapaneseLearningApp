cmake_minimum_required(VERSION 3.10)

project("live2d_sample")

set(APP_NAME native-lib)

# 设置路径变量
set(SDK_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR}/../../../../CubismSdk)
set(CORE_PATH ${SDK_ROOT_PATH}/Core)
set(FRAMEWORK_PATH ${SDK_ROOT_PATH}/Framework)
set(COMMON_SRC_PATH ${SDK_ROOT_PATH}/Samples/Common)
set(THIRD_PARTY_PATH ${SDK_ROOT_PATH}/Samples/OpenGL/thirdParty)
set(STB_PATH ${THIRD_PARTY_PATH}/stb)

# 指定 C++ 标准
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# 添加 Cubism Core 静态库
add_library(Live2DCubismCore STATIC IMPORTED)
set_target_properties(Live2DCubismCore
  PROPERTIES
    IMPORTED_LOCATION
      ${CORE_PATH}/lib/android/${ANDROID_ABI}/libLive2DCubismCore.a
    INTERFACE_INCLUDE_DIRECTORIES ${CORE_PATH}/include
)

# 指定 Cubism Framework 渲染方式
set(FRAMEWORK_SOURCE OpenGL)

# 添加 Cubism Native Framework
add_subdirectory(${FRAMEWORK_PATH} ${CMAKE_CURRENT_BINARY_DIR}/Framework)

# 为 Framework 添加 Android ES2 渲染定义
target_compile_definitions(Framework PUBLIC CSM_TARGET_ANDROID_ES2)

# 创建 Android 应用库
add_library(${APP_NAME} SHARED)

# 添加源文件到应用库
target_sources(${APP_NAME}
  PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/JniBridgeC.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/JniBridgeC.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppDefine.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppDefine.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppDelegate.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppDelegate.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppLive2DManager.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppLive2DManager.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppModel.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppModel.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppPal.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppPal.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppSprite.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppSprite.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppSpriteShader.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppSpriteShader.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppTextureManager.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppTextureManager.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppView.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/LAppView.hpp
)

# 添加 Common 源文件
add_subdirectory(${COMMON_SRC_PATH} ${CMAKE_CURRENT_BINARY_DIR}/src/Common)

# 链接库
target_link_libraries(${APP_NAME}
  Framework
  Live2DCubismCore
  GLESv2
  log
  android
  EGL
)

# 设置包含目录
target_include_directories(${APP_NAME} PRIVATE ${STB_PATH})
target_include_directories(${APP_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# Android 16KB page-size 选项
target_link_options(${APP_NAME} PRIVATE "-Wl,-z,max-page-size=16384")

# ============ VoiceVox JNI 库 ============

add_library(voicevox_jni SHARED
  ${CMAKE_CURRENT_SOURCE_DIR}/voicevox/voicevox_jni.cpp
)

target_include_directories(voicevox_jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(voicevox_jni
  ${log-lib}
  ${android-lib}
)

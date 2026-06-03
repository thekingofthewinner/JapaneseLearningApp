
cmake_minimum_required(VERSION 3.22.1)
project("live2d_sample")

# 1. 设置路径变量
set(SDK_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR}/../../../CubismSdk)
set(CORE_PATH ${SDK_ROOT_PATH}/Core)
set(FRAMEWORK_PATH ${SDK_ROOT_PATH}/Framework)
set(COMMON_SRC_PATH ${SDK_ROOT_PATH}/Samples/Common)
set(THIRD_PARTY_PATH ${SDK_ROOT_PATH}/Samples/OpenGL/thirdParty)
set(STB_PATH ${THIRD_PARTY_PATH}/stb)

# 2. 设置 C++ 标准
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# 3. 添加 Cubism Core 库（静态库）
add_library(Live2DCubismCore STATIC IMPORTED)
set_target_properties(Live2DCubismCore
  PROPERTIES
    IMPORTED_LOCATION
      ${CORE_PATH}/lib/android/${ANDROID_ABI}/libLive2DCubismCore.a
    INTERFACE_INCLUDE_DIRECTORIES ${CORE_PATH}/include
)

# 4. 添加 Cubism Framework
set(FRAMEWORK_SOURCE OpenGL)
add_subdirectory(${FRAMEWORK_PATH} ${CMAKE_CURRENT_BINARY_DIR}/Framework)
target_compile_definitions(Framework PUBLIC CSM_TARGET_ANDROID_ES2)

# 5. 创建应用库
add_library(
    native-lib
    SHARED
)

# 6. 添加示例源文件（包含官方 JNI 桥接）
target_sources(native-lib
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

# 7. 添加 Common 源文件
add_subdirectory(${COMMON_SRC_PATH} ${CMAKE_CURRENT_BINARY_DIR}/src/Common)

# 8. 链接库
target_link_libraries(native-lib
  Framework
  Live2DCubismCore
  GLESv2
  log
  android
  EGL
)

# 9. 设置包含目录
target_include_directories(native-lib PRIVATE ${STB_PATH})
target_include_directories(native-lib PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# 10. Android 16KB page-size 选项
target_link_options(native-lib PRIVATE "-Wl,-z,max-page-size=16384")
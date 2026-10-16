cmake_minimum_required(VERSION 3.21)
project(SonyVegas421 VERSION 1.0 LANGUAGES C CXX RC)

if(NOT WIN32)
    message(FATAL_ERROR "SonyVegas421 is a Windows-only test bridge")
endif()
if(NOT MSVC)
    message(FATAL_ERROR "The audited test bridge requires the MSVC x64 toolchain")
endif()
if(NOT CMAKE_SIZEOF_VOID_P EQUAL 8)
    message(FATAL_ERROR "Only x64 Minecraft/JVM targets are supported")
endif()

set(SONYVEGAS421_JAVA_HOME "$ENV{JAVA_HOME}" CACHE PATH "JDK containing JNI/JVMTI headers")
if(NOT SONYVEGAS421_JAVA_HOME)
    foreach(candidate
            "C:/Program Files/Java/jdk1.8.0_301"
            "C:/Program Files/Java/jdk-17")
        if(EXISTS "${candidate}/include/jni.h")
            set(SONYVEGAS421_JAVA_HOME "${candidate}")
            break()
        endif()
    endforeach()
endif()

find_path(SONYVEGAS421_JNI_INCLUDE jni.h
        HINTS "${SONYVEGAS421_JAVA_HOME}/include" REQUIRED)
find_path(SONYVEGAS421_JNI_MD_INCLUDE jni_md.h
        HINTS "${SONYVEGAS421_JAVA_HOME}/include/win32" REQUIRED)
find_path(SONYVEGAS421_JVMTI_INCLUDE jvmti.h
        HINTS "${SONYVEGAS421_JAVA_HOME}/include" REQUIRED)
find_package(Python3 REQUIRED COMPONENTS Interpreter)

find_program(SONYVEGAS421_JAVAC javac
        HINTS "${SONYVEGAS421_JAVA_HOME}/bin"
        NO_DEFAULT_PATH REQUIRED)
set(SONYVEGAS421_MEMORY_LOADER_SOURCE
        "${CMAKE_CURRENT_SOURCE_DIR}/MemoryJarLoader.java")
set(SONYVEGAS421_MEMORY_LOADER_CLASS
        "${CMAKE_CURRENT_BINARY_DIR}/generated/memory-loader/MemoryJarLoader.class")
set(SONYVEGAS421_MEMORY_LOADER_HEADER
        "${CMAKE_CURRENT_BINARY_DIR}/generated/memory_jar_loader_class.h")
add_custom_command(
        OUTPUT "${SONYVEGAS421_MEMORY_LOADER_HEADER}"
        COMMAND "${CMAKE_COMMAND}" -E make_directory
                "${CMAKE_CURRENT_BINARY_DIR}/generated/memory-loader"
        COMMAND "${SONYVEGAS421_JAVAC}" -encoding UTF-8 -g:none
                -source 8 -target 8
                -d "${CMAKE_CURRENT_BINARY_DIR}/generated/memory-loader"
                "${SONYVEGAS421_MEMORY_LOADER_SOURCE}"
        COMMAND "${Python3_EXECUTABLE}"
                "${CMAKE_CURRENT_SOURCE_DIR}/embed_class.py"
                "${SONYVEGAS421_MEMORY_LOADER_CLASS}"
                "${SONYVEGAS421_MEMORY_LOADER_HEADER}"
        DEPENDS "${SONYVEGAS421_MEMORY_LOADER_SOURCE}"
                "${CMAKE_CURRENT_SOURCE_DIR}/embed_class.py"
        VERBATIM)

set(SONYVEGAS421_LOADER_STRING_SOURCES
        "${CMAKE_CURRENT_SOURCE_DIR}/cleanup.c"
        "${CMAKE_CURRENT_SOURCE_DIR}/control_api_client.c"
        "${CMAKE_CURRENT_SOURCE_DIR}/hwid.c"
        "${CMAKE_CURRENT_SOURCE_DIR}/manual_map.c"
        "${CMAKE_CURRENT_SOURCE_DIR}/module_menu.cpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/secure_transport.c")
set(SONYVEGAS421_OBF_SOURCES
        "${CMAKE_CURRENT_SOURCE_DIR}/dllmain.c"
        ${SONYVEGAS421_LOADER_STRING_SOURCES}
        "${CMAKE_CURRENT_SOURCE_DIR}/injector.c"
        "${CMAKE_CURRENT_SOURCE_DIR}/loader_bootstrap.c"
        "${CMAKE_CURRENT_SOURCE_DIR}/native_bridge.c")
# Every native source with runtime literals stays under the string guard;
# intentional plaintext (sv-raw regions) is limited to C constant
# initializers and the JNI registration table.
set(SONYVEGAS421_STRING_GUARD_SOURCES
        ${SONYVEGAS421_LOADER_STRING_SOURCES}
        "${CMAKE_CURRENT_SOURCE_DIR}/dllmain.c"
        "${CMAKE_CURRENT_SOURCE_DIR}/injector.c"
        "${CMAKE_CURRENT_SOURCE_DIR}/loader_bootstrap.c"
        "${CMAKE_CURRENT_SOURCE_DIR}/native_bridge.c")

set(SONYVEGAS421_IMGUI_SOURCES
        "${CMAKE_CURRENT_SOURCE_DIR}/imgui/imgui.cpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/imgui/imgui_draw.cpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/imgui/imgui_tables.cpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/imgui/imgui_widgets.cpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/imgui/imgui_custom.cpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/imgui/imgui_impl_dx11.cpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/imgui/imgui_impl_opengl3.cpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/imgui/imgui_impl_win32.cpp")
set(SONYVEGAS421_OBF_HEADER
        "${CMAKE_CURRENT_BINARY_DIR}/generated/obf_generated.h")
add_custom_command(
        OUTPUT "${SONYVEGAS421_OBF_HEADER}"
        COMMAND "${Python3_EXECUTABLE}"
                "${CMAKE_CURRENT_SOURCE_DIR}/../tools/wrap_native_strings.py"
                --check ${SONYVEGAS421_STRING_GUARD_SOURCES}
        COMMAND "${Python3_EXECUTABLE}"
                "${CMAKE_CURRENT_SOURCE_DIR}/gen_obf.py"
                "${SONYVEGAS421_OBF_HEADER}"
                ${SONYVEGAS421_OBF_SOURCES}
        DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/gen_obf.py"
                "${CMAKE_CURRENT_SOURCE_DIR}/../tools/wrap_native_strings.py"
                "${CMAKE_CURRENT_SOURCE_DIR}/renamed_classes.txt"
                ${SONYVEGAS421_OBF_SOURCES}
        VERBATIM)

set(SONYVEGAS421_PRODUCT_JAR "" CACHE FILEPATH
        "SonyVegas product JAR embedded into SonyVegas421Native.dll")
if(NOT SONYVEGAS421_PRODUCT_JAR OR NOT EXISTS "${SONYVEGAS421_PRODUCT_JAR}")
    message(FATAL_ERROR
            "SONYVEGAS421_PRODUCT_JAR must name an existing recovered product JAR")
endif()
get_filename_component(SONYVEGAS421_PRODUCT_JAR
        "${SONYVEGAS421_PRODUCT_JAR}" ABSOLUTE)
file(TO_CMAKE_PATH "${SONYVEGAS421_PRODUCT_JAR}" SONYVEGAS421_PRODUCT_JAR_RC_PATH)
configure_file(payload.rc.in generated/payload.rc @ONLY)
set_property(SOURCE "${CMAKE_CURRENT_BINARY_DIR}/generated/payload.rc"
        APPEND PROPERTY OBJECT_DEPENDS "${SONYVEGAS421_PRODUCT_JAR}")

add_library(SonyVegas421Native SHARED
        dllmain.c
        loader_bootstrap.c
        control_api_client.c
        secure_transport.c
        native_bridge.c
        module_menu.cpp
        obf.c
        ${SONYVEGAS421_IMGUI_SOURCES}
        "${SONYVEGAS421_MEMORY_LOADER_HEADER}"
        "${SONYVEGAS421_OBF_HEADER}"
        "${CMAKE_CURRENT_BINARY_DIR}/generated/payload.rc")
target_include_directories(SonyVegas421Native PRIVATE
        "${CMAKE_CURRENT_BINARY_DIR}/generated"
        "${CMAKE_CURRENT_SOURCE_DIR}"
        "${SONYVEGAS421_JNI_INCLUDE}"
        "${SONYVEGAS421_JNI_MD_INCLUDE}"
        "${SONYVEGAS421_JVMTI_INCLUDE}")
target_compile_definitions(SonyVegas421Native PRIVATE
        WIN32_LEAN_AND_MEAN
        _CRT_SECURE_NO_WARNINGS)
target_compile_options(SonyVegas421Native PRIVATE /W4
        $<$<COMPILE_LANGUAGE:CXX>:/EHsc>)
target_link_libraries(SonyVegas421Native PRIVATE
        winhttp user32 shell32 ws2_32 bcrypt crypt32 secur32
        d3d11 dxgi dwmapi windowscodecs ole32 opengl32 gdi32)

add_executable(SonyVegas421Injector
        injector.c
        anti_debug.c
        cleanup.c
        control_api_client.c
        hwid.c
        manual_map.c
        obf.c
        secure_transport.c
        "${SONYVEGAS421_OBF_HEADER}")
target_include_directories(SonyVegas421Injector PRIVATE
        "${CMAKE_CURRENT_BINARY_DIR}/generated"
        "${CMAKE_CURRENT_SOURCE_DIR}")
target_compile_definitions(SonyVegas421Injector PRIVATE
        WIN32_LEAN_AND_MEAN
        _CRT_SECURE_NO_WARNINGS)
target_compile_options(SonyVegas421Injector PRIVATE /W4
        $<$<COMPILE_LANGUAGE:CXX>:/EHsc>)
target_link_libraries(SonyVegas421Injector PRIVATE
        winhttp advapi32 bcrypt crypt32 secur32 user32 shell32
        psapi ws2_32)
target_link_options(SonyVegas421Injector PRIVATE
        "/MANIFESTUAC:level='requireAdministrator' uiAccess='false'")

include(CTest)
if(BUILD_TESTING)
    add_executable(SonyVegas421MenuTest tests/module_menu_test.cpp obf.c
            secure_transport.c
            ${SONYVEGAS421_IMGUI_SOURCES} "${SONYVEGAS421_OBF_HEADER}")
    target_include_directories(SonyVegas421MenuTest PRIVATE
            "${CMAKE_CURRENT_BINARY_DIR}/generated" "${CMAKE_CURRENT_SOURCE_DIR}")
    target_compile_definitions(SonyVegas421MenuTest PRIVATE _CRT_SECURE_NO_WARNINGS)
    target_compile_options(SonyVegas421MenuTest PRIVATE $<$<COMPILE_LANGUAGE:CXX>:/EHsc>)
    target_link_libraries(SonyVegas421MenuTest PRIVATE user32 d3d11 dxgi dwmapi windowscodecs ole32 opengl32 gdi32 ws2_32 secur32 crypt32 bcrypt)
    add_test(NAME SonyVegas421MenuFixtures COMMAND SonyVegas421MenuTest)
    add_executable(SonyVegas421BootstrapTest
            tests/loader_bootstrap_test.c
            loader_bootstrap.c
            obf.c
            "${SONYVEGAS421_OBF_HEADER}")
    target_include_directories(SonyVegas421BootstrapTest PRIVATE
            "${CMAKE_CURRENT_BINARY_DIR}/generated"
            "${CMAKE_CURRENT_SOURCE_DIR}")
    target_compile_definitions(SonyVegas421BootstrapTest PRIVATE
            WIN32_LEAN_AND_MEAN
            _CRT_SECURE_NO_WARNINGS)
    target_compile_options(SonyVegas421BootstrapTest PRIVATE /W4)
    target_link_libraries(SonyVegas421BootstrapTest PRIVATE
            ws2_32 bcrypt crypt32)
    add_test(NAME SonyVegas421BootstrapStandalone
            COMMAND SonyVegas421BootstrapTest standalone)
    add_test(NAME SonyVegas421BootstrapHandoff COMMAND SonyVegas421BootstrapTest)
    add_test(NAME SonyVegas421BootstrapRejectsInvalid
            COMMAND SonyVegas421BootstrapTest invalid)
endif()

set_property(TARGET SonyVegas421Injector PROPERTY
        MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>DLL")
# The injected DLL is manual-mapped into a foreign JVM: it must not depend
# on the shared VC runtime (MSVCP140/VCRUNTIME/api-ms-win-crt-*), which is
# usually absent from the target process and breaks import resolution
# (manual-map stage 5). Link it statically. The injector EXE keeps the
# shared runtime.
set_property(TARGET SonyVegas421Native PROPERTY
        MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

set_target_properties(SonyVegas421Native SonyVegas421Injector PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/raw")
set_target_properties(SonyVegas421Native PROPERTIES OUTPUT_NAME "IllIIlIl")
set_target_properties(SonyVegas421Injector PROPERTIES OUTPUT_NAME "unknown")
foreach(configuration Debug Release RelWithDebInfo MinSizeRel)
    string(TOUPPER "${configuration}" configuration_upper)
    set_target_properties(SonyVegas421Native SonyVegas421Injector PROPERTIES
            RUNTIME_OUTPUT_DIRECTORY_${configuration_upper}
            "${CMAKE_BINARY_DIR}/raw")
endforeach()

# Keep unprotected link outputs separate from the files consumed by the loader.
# A failed VMProtect run must never leave a raw DLL/EXE in dist.
# Local dev machines usually have no licensed VMProtect console: in that case
# fall back to plain (unprotected) copies in dist so prepareInjectionBundle
# stays testable offline. Release builds with the console present keep the
# protected outputs.
set(SONYVEGAS421_VMP_CONSOLE
        "C:/Program Files/VMProtect Ultimate/VMProtect_Con.exe"
        CACHE FILEPATH "Licensed VMProtect console")
file(MAKE_DIRECTORY "${CMAKE_BINARY_DIR}/dist")
if(EXISTS "${SONYVEGAS421_VMP_CONSOLE}")
    set(SONYVEGAS421_VMP_AVAILABLE TRUE)
else()
    set(SONYVEGAS421_VMP_AVAILABLE FALSE)
    message(WARNING "VMProtect console not found at ${SONYVEGAS421_VMP_CONSOLE}; "
            "local build will use unprotected DLL/EXE copies in dist (do not publish)")
endif()

function(add_vmprotected_output target output_name profile)
    set(final "${CMAKE_BINARY_DIR}/dist/${output_name}")
    set(temp "${CMAKE_BINARY_DIR}/dist/${output_name}.protecting")
    add_custom_command(
            OUTPUT "${final}"
            COMMAND "${CMAKE_COMMAND}" -E rm -f "${temp}"
            COMMAND "${SONYVEGAS421_VMP_CONSOLE}"
                    "$<TARGET_FILE:${target}>" "${temp}"
                    -pf "${final}.vmp"
            COMMAND "${CMAKE_COMMAND}"
                    "-DINPUT=${temp}" "-DOUTPUT=${final}"
                    "-DMAX_BYTES=$<IF:$<STREQUAL:${output_name},IllIIlIl.dll>,33554432,0>"
                    -P "${CMAKE_CURRENT_SOURCE_DIR}/promote_vmprotected.cmake"
            COMMAND "${CMAKE_COMMAND}" -E rm -f "${temp}"
            DEPENDS "$<TARGET_FILE:${target}>"
                    "${CMAKE_CURRENT_SOURCE_DIR}/${profile}"
                    "${CMAKE_CURRENT_SOURCE_DIR}/promote_vmprotected.cmake"
            VERBATIM)
    add_custom_target(${target}Protected ALL DEPENDS "${final}")
    add_dependencies(${target}Protected ${target})
endfunction()

function(add_unprotected_fallback target output_name)
    set(final "${CMAKE_BINARY_DIR}/dist/${output_name}")
    add_custom_command(
            OUTPUT "${final}"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different
                    "$<TARGET_FILE:${target}>" "${final}"
            DEPENDS ${target}
            VERBATIM)
    add_custom_target(${target}Protected ALL DEPENDS "${final}")
    add_dependencies(${target}Protected ${target})
endfunction()

if(SONYVEGAS421_VMP_AVAILABLE)
    configure_file("${CMAKE_CURRENT_SOURCE_DIR}/vmprotect-dll.vmp"
            "${CMAKE_BINARY_DIR}/dist/IllIIlIl.dll.vmp" COPYONLY)
    configure_file("${CMAKE_CURRENT_SOURCE_DIR}/vmprotect-exe.vmp"
            "${CMAKE_BINARY_DIR}/dist/unknown.exe.vmp" COPYONLY)
    add_vmprotected_output(SonyVegas421Native "IllIIlIl.dll" "vmprotect-dll.vmp")
    add_vmprotected_output(SonyVegas421Injector "unknown.exe" "vmprotect-exe.vmp")
else()
    add_unprotected_fallback(SonyVegas421Native "IllIIlIl.dll")
    add_unprotected_fallback(SonyVegas421Injector "unknown.exe")
endif()

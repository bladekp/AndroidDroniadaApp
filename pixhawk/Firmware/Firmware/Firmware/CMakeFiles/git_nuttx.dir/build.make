# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 3.8

# Delete rule output on recipe failure.
.DELETE_ON_ERROR:


#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:


# Remove some rules from gmake that .SUFFIXES does not remove.
SUFFIXES =

.SUFFIXES: .hpux_make_needs_suffix_list


# Suppress display of executed commands.
$(VERBOSE).SILENT:


# A target that is always out of date.
cmake_force:

.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /usr/share/cmake-3.8.1-Linux-x86_64/bin/cmake

# The command to remove a file.
RM = /usr/share/cmake-3.8.1-Linux-x86_64/bin/cmake -E remove -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /home/bladekp/organizacje/softmis/projekty/droniada2017/IoT/pixhawk/Firmware/Firmware/Firmware

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /home/bladekp/organizacje/softmis/projekty/droniada2017/IoT/pixhawk/Firmware/Firmware/Firmware

# Utility rule file for git_nuttx.

# Include the progress variables for this target.
include CMakeFiles/git_nuttx.dir/progress.make

CMakeFiles/git_nuttx: git_init_NuttX.stamp


git_init_NuttX.stamp: .gitmodules
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --blue --bold --progress-dir=/home/bladekp/organizacje/softmis/projekty/droniada2017/IoT/pixhawk/Firmware/Firmware/Firmware/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Generating git_init_NuttX.stamp"
	touch /home/bladekp/organizacje/softmis/projekty/droniada2017/IoT/pixhawk/Firmware/Firmware/Firmware/git_init_NuttX.stamp

git_nuttx: CMakeFiles/git_nuttx
git_nuttx: git_init_NuttX.stamp
git_nuttx: CMakeFiles/git_nuttx.dir/build.make

.PHONY : git_nuttx

# Rule to build all files generated by this target.
CMakeFiles/git_nuttx.dir/build: git_nuttx

.PHONY : CMakeFiles/git_nuttx.dir/build

CMakeFiles/git_nuttx.dir/clean:
	$(CMAKE_COMMAND) -P CMakeFiles/git_nuttx.dir/cmake_clean.cmake
.PHONY : CMakeFiles/git_nuttx.dir/clean

CMakeFiles/git_nuttx.dir/depend:
	cd /home/bladekp/organizacje/softmis/projekty/droniada2017/IoT/pixhawk/Firmware/Firmware/Firmware && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /home/bladekp/organizacje/softmis/projekty/droniada2017/IoT/pixhawk/Firmware/Firmware/Firmware /home/bladekp/organizacje/softmis/projekty/droniada2017/IoT/pixhawk/Firmware/Firmware/Firmware /home/bladekp/organizacje/softmis/projekty/droniada2017/IoT/pixhawk/Firmware/Firmware/Firmware /home/bladekp/organizacje/softmis/projekty/droniada2017/IoT/pixhawk/Firmware/Firmware/Firmware /home/bladekp/organizacje/softmis/projekty/droniada2017/IoT/pixhawk/Firmware/Firmware/Firmware/CMakeFiles/git_nuttx.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : CMakeFiles/git_nuttx.dir/depend


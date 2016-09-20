@echo off
:loop 
adb shell procrank | grep %1
call:sleep %2*1000

goto loop

:sleep
set tmp="%temp%\tmp.vbs"
echo wscript.sleep %1>%tmp%&%tmp%&del %tmp%
goto :eof
SET OUTPUT_PATH=D:\Documents\workspace\
SET PROJECT_PATH=D:\Documents\iappli\

COPY /Y %PROJECT_PATH%FiveTreasureIslands\bin\FiveTreasureIslands.jam %OUTPUT_PATH%FIApps\
COPY /Y %PROJECT_PATH%FiveTreasureIslands\bin\FiveTreasureIslands.jar %OUTPUT_PATH%FIApps\
COPY /Y %OUTPUT_PATH%FiveTreasureIslands\bin\FI.jar %OUTPUT_PATH%FIApps\


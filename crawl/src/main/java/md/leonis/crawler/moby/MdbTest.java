package md.leonis.crawler.moby;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;

import java.io.File;
import java.io.IOException;

public class MdbTest {

    public static void main(String[] args) throws IOException {

        Database database = DatabaseBuilder.open(new File("D:\\rom-shingler\\Atari 7800\\Atari 7800.mdb"));

        System.out.println(database.getTableNames());
        // [Config, Crackers, Difficulty, Extras, Games, Genres, Languages, Music, Musicians, PGenres, Programmers, Publishers, ViewData, ViewFilters, Years]

        /*for (String db : database.getTableNames()) {
            Table table = database.getTable(db);
            System.out.println(table.getColumns());
        }*/

        System.out.println(database.getTable("Games").getColumns().stream().map(Column::getName));

        /*for(Row row : database.getTable("Games")) {
            System.out.println("Look ma, a row: " + row);
        }*/

        for(Row row : database.getTable("Languages")) {
            System.out.println("Look ma, a row: " + row);
        }

        // GA_Id=66,
        // Name=Karateka,
        // YE_Id=28,
        // Comment=,
        // Filename=Karateka.zip,
        // FileToRun=,
        // FilenameIndex=0,
        // ScrnshotFilename=Karateka.png,
        // MU_Id=38,
        // GE_Id=4,
        // PU_Id=38,
        // DI_Id=8,
        // CR_Id=70,
        // SidFilename=,
        // DateLastPlayed=07-05-2006,
        // TimesPlayed=2,
        // CCode=2,
        // HighScore=,
        // FA=true,
        // SA=false,
        // Fav=false,
        // PR_Id=27,
        // LA_Id=2,
        // Extras=true,
        // Classic=false,
        // Rating=0,
        // V_LoadingScreen=false,
        // V_HighScoreSaver=false,
        // V_IncludedDocs=false,
        // V_PalNTSC=2,
        // V_TrueDriveEmu=true,
        // V_Length=1,
        // V_Trainers=49,
        // PlayersFrom=1,
        // PlayersTo=1,
        // PlayersSim=false,
        // V_Comment=,
        // Adult=false,
        // MemoText=Description
        //    The game was originally designed by for Broderbund in 1984 by Jordan Mechner, who went on to further fame with Prince of Persia. The 7800 version was adapted by Ibid Inc. in 1987, and unfortunately does not live up to other versions.
        //
        //    ,
        //    Prequel=0,
        //    Sequel=0,
        //    Related=0,
        //    Control=1,
        //    CRC=,
        //    Filesize=0,
        //    Version=1,
        //    Gemus=,
        //    V_LengthType=2
        //    }]
    }
}

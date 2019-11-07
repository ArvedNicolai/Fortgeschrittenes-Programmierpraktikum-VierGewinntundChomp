import java.util.Scanner;

abstract class Spiel
{
    public boolean gewonnen = false, unentschieden = false;
    public Spieler[] spieler = new Spieler[2];
    public Spielfeld feld;
    public boolean ersterZug = true;
    public Spiel(String name1, String name2, Boolean hatBot, int höhe, int breite)
    {
        spieler[0] = new Spieler(name1, "G", false);
        spieler[1] = new Spieler(name2, "R", hatBot);
    }
    abstract void Spielzug(Spieler spieler, int x, int y);
    abstract void Durchgang();
}

class Spieler
{
    public String name;
    public String farbe;
    public boolean istBot;
    public Spieler(String name, String farbe, boolean istBot)
    {
        this.name = name;
        this.farbe = farbe;
        this.istBot = istBot;
    }
}

abstract class Spielfeld
{
    int breite, höhe;
    String[][] status;
    abstract void Darstellen();
}

interface Protokolierbar
{
    public Speicher stack = null;
    abstract void SpielzugHinzu();
    abstract void SpielzugEntfern();
}

class Knoten
{
    public Knoten next;
    public String[][] status;
    public Knoten (String[][] status, Knoten next)
    {
        this.status = status;
        this.next = next;
    }
}

class Speicher
{
    public Knoten top;
    public Speicher()
    {
        top = null;
    }
    public void push(String[][] status)
    {
        top = new Knoten (status, top);
    }
    public String[][] pop()
    {
        if (top == null)
        {
            return null;
        }
        String[][] stat = top.status;
        top = top.next;
        return stat;
    }
    public String[][] peek()
    {
        return top.status;
    }
}
// Ende Aufgabe 1
// Anfang von paar Methoden
class Methoden
{
    public static int DropChip4G(Spielfeld feld, int y)
    {
        int x = 0;
        while (x < feld.höhe)
        {
            if (feld.status[x][y] == "W")
            {
                break;
            }
            x++;
        }
        return x;
    }
    public static String[] Winable4G(Spielfeld feld, Spieler spieler)
    {
        String[] winable = new String[feld.breite];
        for (int y = 0; y < feld.breite; y++)
        {
            winable[y] = CheckWinable4G(feld, spieler, DropChip4G(feld,y), y);
        }
        return winable;
    }
    public static String CheckWinable4G(Spielfeld feld, Spieler spieler, int x, int y)
    {
        String s;
        if (x >= feld.höhe || x < 0)
        {
            s = "I";
        }
        else
        {
            String original = feld.status[x][y];
            feld.status[x][y] = spieler.farbe;
            if (CheckWin4G(feld, spieler))
            {
                s = spieler.farbe;
            }
            else
            {
                s = "X";
            }
            feld.status[x][y] = original;
        }
        return s;
    }
    public static boolean CheckWin4G(Spielfeld feld, Spieler spieler)
    {
        for (int i = 0; i < feld.höhe; i++)     //links nach rechts
        {
            for (int j = 0; j < feld.breite - 3; j++)
            {
                if (feld.status[i][j] == spieler.farbe && feld.status[i][j] == feld.status[i][j+1] && feld.status[i][j] == feld.status[i][j+2] && feld.status[i][j] == feld.status[i][j+3])
                {
                    return true;
                }
            }
        }
        for (int i = 0; i < feld.höhe - 3; i++)       //unten nach oben
        {
            for (int j = 0; j < feld.breite; j++)
            {
                if (feld.status[i][j] == spieler.farbe && feld.status[i][j] == feld.status[i+1][j] && feld.status[i][j] == feld.status[i+2][j] && feld.status[i][j] == feld.status[i+3][j])
                {
                    return true;
                }
            }
        }
        for (int i = 0; i < feld.höhe - 3; i++)       //obenrechts nach untenlinks
        {
            for (int j = 0; j < feld.breite - 3; j++)
            {
                if (feld.status[i][j] == spieler.farbe && feld.status[i][j] == feld.status[i+1][j+1] && feld.status[i][j] == feld.status[i+2][j+2] && feld.status[i][j] == feld.status[i+3][j+3])
                {
                    return true;
                }
            }
        }
        for (int i = 0; i < feld.höhe - 3; i++)         //obenlinks nach untenrechts check
        {
            for (int j = feld.breite - 1; j > 2; j--)
            {
                if (feld.status[i][j] == spieler.farbe && feld.status[i][j] == feld.status[i+1][j-1] && feld.status[i][j] == feld.status[i+2][j-2] && feld.status[i][j] == feld.status[i+3][j-3])
                {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean CheckWinChomp(Spielfeld feld)
    {
        if (feld.status[feld.höhe-2][0] != "W" && feld.status[feld.höhe-1][1] != "W")
        {
            return true;
        }
        return false;
    }
}
// Ende von den paar Methoden
// Anfang Aufgabe 2

class VierGewinnt extends Spiel implements Protokolierbar
{
    Scanner scan = new Scanner(System.in);
    public VierGewinnt(String name1, String name2, Boolean hatBot, int höhe, int breite)
    {
        super(name1, name2, hatBot, höhe, breite);
        feld = new Spielfeld()
        {
            @Override
            void Darstellen()
            {
                for (int i = höhe - 1; i >= 0; i--)
                {
                    System.out.print("\n");
                    for (int j = 0; j < breite; j++)
                    {

                        System.out.print(status[i][j] + "\t");
                    }

                }
                System.out.print("\n");
                for (int j = 1; j <= breite; j++)
                {
                    System.out.print(j + "\t");
                }
                System.out.println("\n");
            }
        };
        feld.höhe = höhe;
        feld.breite = breite;
        feld.status = new String[höhe][breite];
        for (int i = 0; i < höhe; i++)
        {
            for (int j = 0; j < breite; j++)
            {
                feld.status[i][j] = "W";
            }
        }
    }

    public void Spielzug(Spieler spieler, int x, int y)
    {
        feld.status[x][y-1] = spieler.farbe;
    }
    public void Durchgang()
    {
        for (int i = 0; i < 2; i++)
        {
            int spalte;
            if (this.ersterZug)     //Ausnahme erster Zug
            {
                i = (int) (Math.random() + .5);
                this.ersterZug = false;
                System.out.println(spieler[i].name + " fängt an.");
            }
            String[] winablePlayer = Methoden.Winable4G(feld, spieler[i]);
            String[] winableEnemy = Methoden.Winable4G(feld, spieler[(i+1) % 2]);
            for (int k = 0; k < feld.breite; k++)   //Check ob es ein Unentschieden ist bzw. nichts mehr ins Feld reinpasst
            {
                this.unentschieden = true;
                if (winablePlayer[k] != "I")
                {
                    this.unentschieden = false;
                    break;
                }
            }
            if (!this.unentschieden)
            {
                feld.Darstellen();
                System.out.println(spieler[i].name + " (" + spieler[i].farbe + ") ist dran");
                while (true)
                {
                    if (!spieler[i].istBot)     //Anfang Auswahl der Spalte des menschlichen Spielers
                    {
                        System.out.println("In welche Spalte wollen Sie reinwerfen");
                        System.out.print("Spalte: ");
                        try
                        {
                            spalte = scan.nextInt();
                        }
                        catch (java.util.InputMismatchException e)
                        {
                            String bad_input = scan.next();
                            System.out.println("Bad input: " + bad_input);
                            continue;
                        }
                    }
                    else    //Anfang Auswahl der Spalte des Computers
                    {
                        spalte = (int)(Math.random()*feld.breite+1);    //zufällige Spalte wird gewählt
                        for (int k = 1; k <= feld.breite; k++)
                        {
                            if (winableEnemy[k-1] == spieler[(i+1) % 2].farbe)     //falls der Gegner gewinnen könnte wird es damit verhindert
                            {
                                spalte = k;
                            }
                        }
                        for (int k = 1; k <= feld.breite; k++)
                        {
                            if (winablePlayer[k-1] == spieler[i].farbe)    //falls der Computer gewinnen kann wird er es damit tun
                            {
                                spalte = k;
                            }
                        }
                    }
                    if (spalte > 0 && spalte <= feld.breite && Methoden.DropChip4G(feld, (spalte-1)) < feld.höhe)   //Check eine gültige Spalte ausgewählt wurde
                    {
                        if (winablePlayer[spalte-1] == spieler[i].farbe)    //Check ob das Spiel gewonnen wurde
                        {
                            this.gewonnen = true;
                        }
                        break;
                    }
                    else
                    {
                        if (!spieler[i].istBot)
                        {
                            System.out.println("Auswahl ungültig. Biite wählen Sie eine andere Spalte.");
                        }
                    }
                }
                Spielzug(spieler[i], Methoden.DropChip4G(feld, (spalte-1)), spalte);
            }
            else
            {
                feld.Darstellen();
                System.out.println("Unentschieden, keiner hat gewonnen.");
                break;
            }
            if (this.gewonnen)
            {
                feld.Darstellen();
                System.out.println(spieler[i].name + " hat gewonnen");
                break;
            }
        }
    }
    public void SpielzugHinzu()
    {
        stack.push(feld.status);
    }
    public void SpielzugEntfern()
    {
        stack.pop();
        feld.status = stack.peek();
        feld.Darstellen();
    }
}

class Chomp extends Spiel implements Protokolierbar
{
    Scanner scan = new Scanner(System.in);
    public Chomp(String name1, String name2, Boolean hatBot, int höhe, int breite) {
        super(name1, name2, hatBot, höhe, breite);
        feld = new Spielfeld()
        {
            @Override
            void Darstellen()
            {
                for (int i = höhe - 1; i >= 0; i--)
                {
                    System.out.print("\n" + (i + 1) + "\t");
                    for (int j = 0; j < breite; j++)
                    {
                        System.out.print(status[i][j] + "\t");
                    }
                }
                System.out.print("\n");
                for (int j = 0; j <= breite; j++)
                {
                    if (j == 0)
                    {
                        System.out.print(" \t");
                    }
                    else
                    {
                        System.out.print(j + "\t");
                    }
                }
                System.out.println("\n");
            }
        };
        feld.höhe = höhe;
        feld.breite = breite;
        feld.status = new String[höhe][breite];
        for (int i = 0; i < höhe; i++)
        {
            for (int j = 0; j < breite; j++)
            {
                feld.status[i][j] = "W";
            }
        }
        feld.status[feld.höhe-1][0] = "X";
    }

    public void Spielzug(Spieler spieler, int x, int y)
    {
        for (int i = x-1; i >= 0; i--)
        {
            for (int j = y-1; j < feld.breite; j++)
            {
                if (feld.status[i][j] == "W")
                {
                    feld.status[i][j] = spieler.farbe;
                }
            }
        }
        this.gewonnen = Methoden.CheckWinChomp(feld);
    }
    public void Durchgang()
    {
        for (int i = 0; i < 2; i++)
        {
            int reihe, spalte;
            if (this.ersterZug)     //Ausnahme für ersten Zug (zufälliger Spieler beginnt)
            {
                i = (int) (Math.random() + .5);     //Wert zwischen .5 und 1.5 (durch int abrunden 0 und 1)
                this.ersterZug = false;
                System.out.println(spieler[i].name + " fängt an.");
            }
            feld.Darstellen();
            System.out.println(spieler[i].name + " (" + spieler[i].farbe + ") ist dran");
            while (true)
            {
                if (!spieler[i].istBot)     //Anfang Feldauswahl für menschlichen Spieler
                {
                    System.out.println("Welches Feld wollen Sie auswählen");
                    System.out.print("Reihe: ");
                    try
                    {
                        reihe = scan.nextInt();
                    }
                    catch (java.util.InputMismatchException e)
                    {
                        String bad_input = scan.next();
                        System.out.println("Bad input: " + bad_input);
                        continue;
                    }
                    System.out.print("Spalte: ");
                    try
                    {
                        spalte = scan.nextInt();
                    }
                    catch (java.util.InputMismatchException e)
                    {
                        String bad_input = scan.next();
                        System.out.println("Bad input: " + bad_input);
                        continue;
                    }
                }     //Ende Feldauswahl für menschlichen Spieler
                else    // Anfang Verhalten Computer (Chomp)
                {
                    if (feld.status[feld.höhe-1][1] != "W")
                    {
                        reihe = feld.höhe - 1;
                        spalte = 1;

                    }
                    else
                    {
                        if (feld.status[feld.höhe - 2][0] != "W")
                        {
                            reihe = feld.höhe;
                            spalte = 2;
                        }
                        else
                        {
                            reihe = (int)(Math.random()*feld.höhe+1);
                            spalte = (int)(Math.random()*feld.breite+1);
                        }
                    }
                }   // Ende Verhalten Computer (Chomp)
                if (reihe > 0 && reihe <= feld.höhe && spalte > 0 && spalte <= feld.breite && feld.status[reihe-1][spalte-1] == "W" && !(reihe == feld.höhe && spalte == 1))
                {
                    break;
                }
                else
                {
                    if (!spieler[i].istBot)
                    {
                        System.out.println("Auswahl ungültig. Biite nehmen Sie ein anderes Feld.");
                    }
                }
            }
            Spielzug(spieler[i], reihe, spalte);
            if (this.gewonnen)
            {
                feld.Darstellen();
                System.out.println(spieler[i].name + " hat gewonnen");
                break;
            }
        }
    }
    public void SpielzugHinzu()
    {
        stack.push(feld.status);
    }
    public void SpielzugEntfern()
    {
        stack.pop();
        feld.status = stack.peek();
    }
}

public class SpielBlatt1
{
    public static void main(String[] args)
    {
        Scanner scan = new Scanner(System.in);
        Spiel spiel;
        String spieler1, spieler2 = "Spieler 2 Computer";
        boolean hatBot;
        int spielNr, botEingabe, spielHöhe, spielBreite;
        System.out.println("Welches Spiel wollen Sie spielen");
        System.out.println("Schreiben Sie 1 für Vier Gewinnt");
        System.out.println("Schreiben Sie 2 für Chomp");
        while (true)
        {
            try
            {
                spielNr = scan.nextInt();
            }
            catch (java.util.InputMismatchException e)
            {
                System.out.println("Bitte wählen Sie mit 1 oder 2 aus");
                continue;
            }

            if (spielNr == 1 || spielNr == 2)
            {
                break;
            }
        }
        System.out.println("Wie groß soll das Spielfeld sein?");
        while (true)
        {
            System.out.print("Höhe: ");
            try
            {
                spielHöhe = scan.nextInt();
            }
            catch (java.util.InputMismatchException e)
            {
                String bad_input = scan.next();
                System.out.println("Bad input: " + bad_input);
                continue;
            }
            if (spielNr == 1 && spielHöhe >= 5)
                break;
            if (spielNr == 2 && spielHöhe >= 3)
                break;
            if (spielNr == 1)
            {
                System.out.println("Die minimalste Höhe für Vier Gewinnt ist 5");
            }
            if (spielNr == 2)
            {
                System.out.println("Die minimalste Höhe für Chomp ist 3");
            }
        }
        while (true)
        {
            System.out.print("Breite: ");
            try
            {
                spielBreite = scan.nextInt();
            }
            catch (java.util.InputMismatchException e)
            {
                String bad_input = scan.next();
                System.out.println("Bad input: " + bad_input);
                continue;
            }
            if (spielNr == 1 && spielBreite >= 5)
                break;
            if (spielNr == 2 && spielBreite >= 3)
                break;
            if (spielNr == 1)
            {
                System.out.println("Die minimalste Breite für Vier Gewinnt ist 5");
            }
            if (spielNr == 2)
            {
                System.out.println("Die minimalste Breite für Chomp ist 3");
            }
        }
        System.out.println("Soll der zweite Spieler ein Computer sein oder ein Mensch?");
        System.out.println("Schreiben Sie 1 für Computer");
        System.out.println("Schreiben Sie 2 für Mensch");
        while (true)
        {
            try
            {
                botEingabe = scan.nextInt();
            }
            catch (java.util.InputMismatchException e)
            {
                System.out.println("Bitte wählen sie mit 1 oder 2 aus");
                continue;
            }
            if (botEingabe == 1)
            {
                hatBot = true;
                break;
            }
            if (botEingabe == 2)
            {
                hatBot = false;
                break;
            }
        }

        System.out.print("Name Spieler 1: ");
        scan.nextLine();
        spieler1 = "Spieler 1 " + scan.nextLine();
        if (!hatBot)
        {
            System.out.print("Name Spieler 2: ");
            spieler2 = "Spieler 2 " + scan.nextLine();
        }
        if (spielNr == 1)
        {
            spiel = new VierGewinnt(spieler1, spieler2, hatBot, spielHöhe, spielBreite);
        }
        else
        {
            spiel = new Chomp(spieler1, spieler2, hatBot, spielHöhe, spielBreite);
        }
        while (!spiel.gewonnen && !spiel.unentschieden)
        {
            spiel.Durchgang();
        }
    }
}

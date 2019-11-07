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

class Methoden
{
    public static int DropChip4G(Spielfeld feld, int y)
    {
        int x = feld.höhe - 1;
        for (; x >= -1; x--)
        {
            if (x < 0 || feld.status[x][y] == "W")
            {
                break;
            }
        }
        return x;
    }
    public static String[] Winable4G(Spielfeld feld, Spieler spieler)
    {
        String[] winable = new String[feld.breite];
        for (int k = 0; k < feld.breite; k++)
        {
            winable[k] = "X";
        }
        for (int y = 0; y < feld.breite; y++)
        {
            winable[y] = CheckWinable4G(feld, spieler, DropChip4G(feld,y), y);
        }
        return winable;
    }
    public static String CheckWinable4G(Spielfeld feld, Spieler spieler, int x, int y)
    {
        String s = "X";
        if (x >= feld.höhe || x < 0)
        {
            s = "I";
        }
        else
        {
            if (CheckWinableHorizontal(feld, spieler, x, y))
            {
                s = spieler.farbe;
            }
            if (CheckWinableVertikal(feld, spieler, x, y))
            {
                s = spieler.farbe;
            }
            if (CheckWinableSchräg(feld, spieler, x, y))
            {
                s = spieler.farbe;
            }
        }
        return s;
    }
    public static boolean CheckWinableSchräg(Spielfeld feld, Spieler spieler, int x, int y)
    {
        String original = feld.status[x][y];
        feld.status[x][y] = spieler.farbe;
        for (int k = -3; k < 1; k++)
        {
            int s = x + k;
            int t = y + k;
            if (t >= 0 && t < feld.breite - 3 && s >= 0 && s < feld.höhe - 3)
            {
                if (feld.status[s][t] == spieler.farbe && feld.status[s][t] == feld.status[s + 1][t + 1] && feld.status[s][t] == feld.status[s + 2][t + 2] && feld.status[s][t] == feld.status[s + 3][t + 3])
                {
                    feld.status[x][y] = original;
                    return true;
                }
            }
            t = y - k;
            if (t - 3 >= 0 && t < feld.breite && s >= 0 && s < feld.höhe - 3)
            {
                if (feld.status[s][t] == spieler.farbe && feld.status[s][t] == feld.status[s + 1][t - 1] && feld.status[s][t] == feld.status[s + 2][t - 2] && feld.status[s][t] == feld.status[s + 3][t - 3])
                {
                    feld.status[x][y] = original;
                    return true;
                }
            }
        }
        feld.status[x][y] = original;
        return false;
    }
    public static boolean CheckWinableHorizontal(Spielfeld feld, Spieler spieler, int x, int y)
    {
        String original = feld.status[x][y];
        feld.status[x][y] = spieler.farbe;
        for (int k = -3 ; k < 1 ; k++)
        {
            int t = y + k;
            if (t >= 0 && t < feld.breite - 3)
            {
                if (feld.status[x][t] == spieler.farbe && feld.status[x][t] == feld.status[x][t + 1] && feld.status[x][t] == feld.status[x][t + 2] && feld.status[x][t] == feld.status[x][t + 3])
                {
                    feld.status[x][y] = original;
                    return true;
                }
            }
        }
        feld.status[x][y] = original;
        return false;
    }
    public static boolean CheckWinableVertikal(Spielfeld feld, Spieler spieler, int x, int y)
    {
        x = x + 3;
        if (x >= 0 && x < feld.höhe)
        {
            for (int i = 0; i < 3; i++)
            {
                if (feld.status[x - i][y] != spieler.farbe)
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    public static boolean CheckWinChomp(Spielfeld feld)
    {
        if (feld.status[1][0] != "W" && feld.status[1][1] != "W" && feld.status[0][1] != "W")
        {
            return true;
        }
        return false;
    }

    public static boolean CheckAuswahl4G(Spielfeld feld, Spieler spieler, int y)
    {
        if (y > feld.breite)
        {
            return true;
        }
        int x = DropChip4G(feld, y-1);
        if (x >= 0 && x < feld.höhe)
        {
            feld.status[x][y-1] = spieler.farbe;
            return false;
        }
        return true;
    }

    public static boolean CheckAuswahlChomp(Spielfeld feld, int x, int y)
    {
        if (x > feld.breite || y > feld.höhe || feld.status[feld.höhe-y][x-1] != "W" || (x == 1 && y == feld.höhe) || x < 1 || y < 1)
        {
            return true;
        }
        return false;
    }
}

// Anfang Aufgabe 2

class VierGewinnt extends Spiel implements Protokolierbar
{
    Scanner scan = new Scanner(System.in);
    public VierGewinnt(String name1, String name2, Boolean hatBot, int höhe, int breite) {
        super(name1, name2, hatBot, höhe, breite);
        feld = new Spielfeld()
        {
            @Override
            void Darstellen()
            {
                for (int i = 0; i < höhe; i++)
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
        while (Methoden.CheckAuswahl4G(feld, spieler, x))
        {
            if (spieler.istBot)
            {
                x = (int) (Math.random()*feld.breite+1);
            }
            else
            {
                System.out.println("Auswahl ungültig. Biite nehmen Sie eine andere Spalte.");
                try
                {
                    x = scan.nextInt();
                }
                catch (java.util.InputMismatchException e)
                {
                    String bad_input = scan.next();
                    System.out.println("Bad input: " + bad_input);
                    continue;
                }
            }
        }
    }
    public void Durchgang()
    {
        for (int i = 0; i < 2; i++)
        {
            if (this.ersterZug)
            {
                i = (int) (Math.random() + .5);
                this.ersterZug = false;
                System.out.println(spieler[i].name + " fängt an.");
            }
            String[] winablePlayer = Methoden.Winable4G(feld, spieler[i]);
            String[] winableEnemy = Methoden.Winable4G(feld, spieler[(i+1) % 2]);
            for (int k = 0; k < feld.breite; k++)
            {
                this.unentschieden = true;
                if (winablePlayer[k] != "I")
                {
                    this.unentschieden = false;
                    break;
                }
            }
            feld.Darstellen();
            int spalte;
            if (!this.unentschieden)
            {
                if (!spieler[i].istBot)
                {
                    while (true)
                    {
                        System.out.println("In welche Spalte wollen Sie reinwerfen");
                        System.out.print("Spalte: ");
                        try
                        {
                            spalte = scan.nextInt();
                            if (spalte > 0 && spalte <= feld.breite)
                            {
                                break;
                            }
                            else
                            {
                                System.out.println("Spalte ungültig, bitte neue Spalte auswählen");
                            }
                        }
                        catch (java.util.InputMismatchException e)
                        {
                            String bad_input = scan.next();
                            System.out.println("Bad input: " + bad_input);
                            continue;
                        }
                    }
                    if (winablePlayer[spalte-1] == spieler[i].farbe)
                    {
                        this.gewonnen = true;
                    }
                    Spielzug(spieler[i], spalte, 0);
                }
                else
                {
                    boolean zugGemacht = false;
                    for (spalte = 1; spalte <= feld.breite; spalte++)
                    {
                        if (winablePlayer[spalte-1] == spieler[i].farbe)
                        {
                            Spielzug(spieler[i], spalte, 0);
                            zugGemacht = true;
                            this.gewonnen = true;
                            break;
                        }
                    }
                    if (!this.gewonnen || !zugGemacht)
                    {
                        for (spalte = 1; spalte <= feld.breite; spalte++)
                        {
                            if (winableEnemy[spalte-1] == spieler[(i+1) % 2].farbe)
                            {
                                Spielzug(spieler[i], spalte, 0);
                                zugGemacht = true;
                                break;
                            }
                        }
                    }
                    if (!zugGemacht)
                    {
                        Spielzug(spieler[i], (int)(Math.random()*feld.breite+1), 0);
                    }
                }
            }
            if (this.gewonnen)
            {
                feld.Darstellen();
                System.out.println(spieler[i].name + " hat gewonnen");
                break;
            }
            if (this.unentschieden)
            {
                System.out.println("Unentschieden, keiner hat gewonnen.");
                feld.Darstellen();
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
                for (int i = 0; i < höhe; i++)
                {
                    System.out.print("\n" + (höhe - i) + "\t");
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
        feld.status[0][0] = "X";
    }

    public void Spielzug(Spieler spieler, int x, int y)
    {
        while (Methoden.CheckAuswahlChomp(feld, x, y))
        {
            if (spieler.istBot)
            {
                x = (int)(Math.random()*feld.breite+1);
                y = (int)(Math.random()*feld.höhe+1);
            }
            else
            {
                System.out.println("Auswahl ungültig. Biite nehmen Sie ein anderes Feld.");
                System.out.print("Neue Reihe: ");
                try
                {
                    y = scan.nextInt();
                }
                catch (java.util.InputMismatchException e)
                {
                    String bad_input = scan.next();
                    System.out.println("Bad input: " + bad_input);
                    continue;
                }
                System.out.print("Neue Spalte: ");
                try
                {
                    x = scan.nextInt();
                }
                catch (java.util.InputMismatchException e)
                {
                    String bad_input = scan.next();
                    System.out.println("Bad input: " + bad_input);
                    continue;
                }
            }
        }
        for (int i = y-1; i >= 0; i--)
        {
            for (int j = x-1; j < feld.breite; j++)
            {
                if (feld.status[feld.höhe-i-1][j] == "W")
                {
                    feld.status[feld.höhe-i-1][j] = spieler.farbe;
                }
            }
        }
        this.gewonnen = Methoden.CheckWinChomp(feld);
    }
    public void Durchgang()
    {
        for (int i = 0; i < 2; i++)
        {
            if (this.ersterZug)
            {
                i = (int) (Math.random() + .5);
                this.ersterZug = false;
                System.out.println(spieler[i].name + " fängt an.");
            }
            feld.Darstellen();
            if (!spieler[i].istBot)
            {
                System.out.println("Welches Feld wollen Sie auswählen");
                int reihe, spalte;
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
                Spielzug(spieler[i], spalte, reihe);
            }
            else
            {
                boolean zugDruchgeführt = false;
                if (feld.status[0][1] != "W" && !zugDruchgeführt)
                {
                    Spielzug(spieler[i],2,feld.höhe);
                    zugDruchgeführt = true;
                }
                if (feld.status[1][0] != "W" && !zugDruchgeführt)
                {
                    System.out.println("im in");
                    Spielzug(spieler[i],1,feld.höhe - 1);
                    zugDruchgeführt = true;
                }
                if (!zugDruchgeführt)
                {
                    Spielzug(spieler[i],(int)(Math.random()*feld.breite+1),(int)(Math.random()*feld.höhe+1));
                }
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
    }
}

public class UpdatedSpielBlatt1
{
    public static void main(String[] args)
    {
        Scanner scan = new Scanner(System.in);
        Spiel spiel;
        String spieler1, spieler2 = "Computer";
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
                String bad_input = scan.next();
                System.out.println("Bad input: " + bad_input);
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
                String bad_input = scan.next();
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

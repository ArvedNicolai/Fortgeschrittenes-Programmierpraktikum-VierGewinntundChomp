import java.util.Scanner;

abstract class Spiel
{
    public boolean gewonnen = false;
    public Spieler[] spieler = new Spieler[2];
    public Spielfeld feld;
    public Spiel(String name1, String name2, Boolean hatBot, int höhe, int breite)
    {
        spieler[0] = new Spieler(name1, "G", false);
        spieler[1] = new Spieler(name2, "R", hatBot);
        feld = new Spielfeld(höhe, breite);
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

class Spielfeld
{
    int breite, höhe;
    String[][] status;
    public Spielfeld(int x, int y)
    {
        this.höhe = x;
        this.breite = y;
        status = new String[höhe][breite];
        for (int i = 0; i < höhe; i++)
        {
            for (int j = 0; j < breite; j++)
            {
                status[i][j] = "W";
            }
        }
    }
    public void Darstellen()
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
        System.out.print("\n");
    }
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
}
// Ende Aufgabe 1

class Methoden
{
    public static boolean CheckWin4G(Spielfeld feld)
    {
        for (int i = 0; i < feld.höhe; i++)     //links nach rechts check
        {
            for (int j = 0; j < feld.breite - 3; j++)
            {
                if (feld.status[i][j] != "W" && feld.status[i][j] == feld.status[i][j+1] && feld.status[i][j] == feld.status[i][j+2] && feld.status[i][j] == feld.status[i][j+3])
                {
                    return true;
                }
            }
        }
        for (int i = 0; i < feld.breite; i++)       //oben nach unten check
        {
            for (int j = 0; j < feld.höhe - 3; j++)
            {
                if (feld.status[j][i] != "W" && feld.status[j][i] == feld.status[j+1][i] && feld.status[j][i] == feld.status[j+2][i] && feld.status[j][i] == feld.status[j+3][i])
                {
                    return true;
                }
            }
        }
        for (int i = 0; i < feld.breite - 3; i++)       //oben-links nach unten-rechts check
        {
            for (int j = 0; j < feld.höhe - 3; j++)
            {
                if (feld.status[j][i] != "W" && feld.status[j][i] == feld.status[j+1][i+1] && feld.status[j][i] == feld.status[j+2][i+2] && feld.status[j][i] == feld.status[j+3][i+3])
                {
                    return true;
                }
            }
        }
        for (int i = 0; i < feld.höhe; i++)         //oben-rechts nach unten-links check
        {
            for (int j = feld.breite; j < 3; j--)
            {
                if (feld.status[i][j] != "W" && feld.status[i][j] == feld.status[i+1][j-1] && feld.status[i][j] == feld.status[i+2][j-2] && feld.status[i][j] == feld.status[i+3][j-3])
                {
                    return true;
                }
            }
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

    public static boolean CheckAuswahl4G(Spielfeld feld, Spieler spieler, int x)
    {
        if (x > feld.breite)
        {
            return true;
        }
        for (int i = feld.höhe - 1; i >= 0; i--)
        {
            if (feld.status[i][x-1] == "W")
            {
                feld.status[i][x-1] = spieler.farbe;
                return false;
            }
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
    }

    public void Spielzug(Spieler spieler, int x, int y)
    {
        while (Methoden.CheckAuswahl4G(feld, spieler, x))
        {
            if (spieler.istBot)
            {
                x = (int) Math.random() * feld.breite + 1;
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
        this.gewonnen = Methoden.CheckWin4G(feld);
        feld.Darstellen();
    }
    public void Durchgang()
    {
        for (int i = 0; i < 2; i++)
        {
            feld.Darstellen();
            if (!spieler[i].istBot)
            {
                int spalte;
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
                Spielzug(spieler[i], spalte, 0);
            }
            else
            {
                Spielzug(spieler[i], (int)(Math.random()*feld.breite+1), 0);
            }
            if (this.gewonnen)
            {
                System.out.println("Spieler " + (i+1) + " " + spieler[i].name + " hat gewonnen");
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
    }
}

class Chomp extends Spiel implements Protokolierbar
{
    Scanner scan = new Scanner(System.in);
    public Chomp(String name1, String name2, Boolean hatBot, int höhe, int breite) {
        super(name1, name2, hatBot, höhe, breite);
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
        feld.Darstellen();
    }
    public void Durchgang()
    {
        for (int i = 0; i < 2; i++)
        {
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
                Spielzug(spieler[i],(int)(Math.random()*feld.breite+1),(int)(Math.random()*feld.höhe+1));
            }
            if (this.gewonnen)
            {
                System.out.println("Spieler " + (i+1) + " " + spieler[i].name + " hat gewonnen");
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
    }
}

public class SpielBlatt1
{
    public static void main(String [] args)
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
        spieler1 = scan.next();
        if (!hatBot)
        {
            System.out.print("Name Spieler 2: ");
            spieler2 = scan.next();
        }
        if (spielNr == 1)
        {
            spiel = new VierGewinnt(spieler1, spieler2, hatBot, spielHöhe, spielBreite);
        }
        else
        {
            spiel = new Chomp(spieler1, spieler2, hatBot, spielHöhe, spielBreite);
        }
        while (!spiel.gewonnen)
        {
            spiel.Durchgang();
        }
    }
}

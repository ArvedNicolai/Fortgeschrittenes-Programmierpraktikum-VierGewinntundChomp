import java.util.Scanner;

abstract class Spiel
{
    public boolean gewonnen = false;
    public Spieler player1, player2;
    public Spielfeld feld;
    public Spiel(String name1, String name2, Boolean hatBot, int höhe, int breite)
    {
        player1 = new Spieler(name1, "G", false);
        player2 = new Spieler(name2, "R", hatBot);
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
    public boolean zugDurchgeführt = false;
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
        System.out.println("\n");
        for (int j = 0; j <= breite; j++)
        {
            System.out.print(j + "\t");
        }
        for (int i = 0; i < höhe; i++)
        {
            System.out.print("\n" + (i+1) + "\t");
            for (int j = 0; j < breite; j++)
            {
                System.out.print(status[i][j] + "\t");
            }
        }
        System.out.println("\n");
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

// Anfang Aufgabe 2

class VierGewinnt extends Spiel implements Protokolierbar
{
    public VierGewinnt(String name1, String name2, Boolean hatBot, int höhe, int breite) {
        super(name1, name2, hatBot, höhe, breite);
    }

    public void Spielzug(Spieler spieler, int x, int y)
    {
        for (int i = feld.höhe - 1; i >= 0; i--)
        {
            if (feld.status[i][x-1] == "W")
            {
                feld.status[i][x-1] = spieler.farbe;
                break;
            }
        }
        spieler.zugDurchgeführt = true;
        for (int i = 0; i < feld.höhe; i++)     //links nach rechts check
        {
            for (int j = 0; j < feld.breite - 3; j++)
            {
                if (!this.gewonnen && feld.status[i][j] != "W" && feld.status[i][j] == feld.status[i][j+1] && feld.status[i][j] == feld.status[i][j+2] && feld.status[i][j] == feld.status[i][j+3])
                {
                    this.gewonnen = true;
                }
            }
        }
        for (int i = 0; i < feld.breite; i++)       //oben nach unten check
        {
            for (int j = 0; j < feld.höhe - 3; j++)
            {
                if (!this.gewonnen && feld.status[j][i] != "W" && feld.status[j][i] == feld.status[j+1][i] && feld.status[j][i] == feld.status[j+2][i] && feld.status[j][i] == feld.status[j+3][i])
                {
                    this.gewonnen = true;
                }
            }
        }
        for (int i = 0; i < feld.breite - 3; i++)       //oben-links nach unten-rechts check
        {
            for (int j = 0; j < feld.höhe - 3; j++)
            {
                if (!this.gewonnen && feld.status[j][i] != "W" && feld.status[j][i] == feld.status[j+1][i+1] && feld.status[j][i] == feld.status[j+2][i+2] && feld.status[j][i] == feld.status[j+3][i+3])
                {
                    this.gewonnen = true;
                }
            }
        }
        for (int i = 0; i < feld.höhe; i++)         //oben-rechts nach unten-links check
        {
            for (int j = feld.breite; j < 3; j--)
            {
                if (!this.gewonnen && feld.status[i][j] != "W" && feld.status[i][j] == feld.status[i+1][j-1] && feld.status[i][j] == feld.status[i+2][j-2] && feld.status[i][j] == feld.status[i+3][j-3])
                {
                    this.gewonnen = true;
                }
            }
        }
        feld.Darstellen();
    }
    public void Durchgang()
    {
        if (player1.zugDurchgeführt && player2.zugDurchgeführt)
        {
            player1.zugDurchgeführt = false;
            player2.zugDurchgeführt = false;
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
    public Chomp(String name1, String name2, Boolean hatBot, int höhe, int breite) {
        super(name1, name2, hatBot, höhe, breite);
    }

    public void Spielzug(Spieler spieler, int x, int y)
    {
        if (feld.status[1][0] != "W" && feld.status[1][1] != "W" && feld.status[0][1] != "W")
        {
            this.gewonnen = true;
        }
    }
    public void Durchgang()
    {
        if (player1.zugDurchgeführt && player2.zugDurchgeführt)
        {
            player1.zugDurchgeführt = false;
            player2.zugDurchgeführt = false;
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

            if (spielNr == 1)
            {
                System.out.println("Vier Gewinnt wurde gewählt");
                break;
            }
            if (spielNr == 2)
            {
                System.out.println("Chomp wurde gewählt.");
                break;
            }
            System.out.print("fck");
        }
        System.out.println("Wie hoch soll das Spielfeld sein?");
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
        System.out.println("Wie breit soll das Spielfeld sein?");
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
                System.out.println("Bad input: " + bad_input);
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
        spiel = new VierGewinnt(spieler1, spieler2, hatBot, spielHöhe, spielBreite);
        spiel.feld.Darstellen();
        if (spielNr == 1)
        {
            spiel = new VierGewinnt(spieler1, spieler2, hatBot, spielHöhe, spielBreite);
            while (!spiel.gewonnen)
            {
                System.out.println("In welche Spalte wollen Sie reinwerfen");
                System.out.print("Spalte: ");
                int spalte1 = scan.nextInt();
                spiel.Spielzug(spiel.player1, spalte1, 0);
                if (!spiel.player2.istBot)
                {
                    System.out.println("In welche Spalte wollen Sie reinwerfen");
                    System.out.print("Spalte: ");
                    int spalte2 = scan.nextInt();
                    spiel.Spielzug(spiel.player2, spalte2, 0);
                }
                else
                {
                    spiel.Spielzug(spiel.player2, (int)(Math.random()*((spiel.feld.breite-1)+1))+1, 0);
                }
            }
        }
        else
        {
            spiel = new Chomp(spieler1, spieler2, hatBot, spielHöhe, spielBreite);
            while (!spiel.gewonnen)
            {
                System.out.println("Welches Feld wollen Sie auswählen");
                System.out.print("Reihe: ");
                int reihe1 = scan.nextInt();
                System.out.print("Spalte: ");
                int spalte1 = scan.nextInt();
                spiel.Spielzug(spiel.player1, spalte1, reihe1);
                if (!spiel.player2.istBot)
                {
                    System.out.println("Welches Feld wollen Sie auswählen");
                    System.out.print("Reihe: ");
                    int reihe2 = scan.nextInt();
                    System.out.print("Spalte: ");
                    int spalte2 = scan.nextInt();
                    spiel.Spielzug(spiel.player2, spalte2, reihe2);
                }
                else
                {
                    spiel.Spielzug(spiel.player2,(int)(Math.random()*((spiel.feld.breite-1)+1))+1,(int)(Math.random()*((spiel.feld.höhe-1)+1))+1);
                }
            }
        }
    }
}

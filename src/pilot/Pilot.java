package pilot;

import gestore.GestoreGrafo;
import grafo.Arco;
import grafo.Colore;
import grafo.Grafo;
import grafo.GrafoColorato;
import grafo.Nodo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 *
 * @author Orazio
 */
public class Pilot {

    GrafoColorato grafo;

    public Pilot(GrafoColorato grafo) {
        this.grafo = grafo;
    }

    public int esegui(ArrayList<Integer> col) {
        int sol;
        
        GrafoColorato mlst = new GrafoColorato(grafo.getNodi(), grafo.getListaColori().size());
        ArrayList<Arco> tmpArchi = grafo.getCopiaArchi();
        ArrayList<Colore> tmpColori = grafo.getCopiaColori();

        GestoreGrafo gestoreMlst = new GestoreGrafo(mlst);

        ArrayList<Arco> edgeWithMinColors;
        ArrayList<Integer> indexOfEdgeWithMinColors;
        
        for (int i=0; i<col.size(); i++) {
            rimuoviColore(tmpArchi, tmpColori, col.get(i));
        }

        //Ciclo, fin quando mlst non e' connesso
        while (!gestoreMlst.connesso()) {
            //Prelevo gli indici degli archi con colore minimo, poichè le operazioni effettuate su un arraylist 
            //con gli indici (get, set) hanno complessità O(1), diversamente dalle operazioni effettuate con gli oggetti
            indexOfEdgeWithMinColors = getIndexOfEdgesWithMinNumberOfColors(tmpArchi);

            //Prendo gli archi con il minor numero di colori
            edgeWithMinColors = getEdgesWithMinNumberOfColors(tmpArchi, indexOfEdgeWithMinColors);

            //Se gli archi minimi non hanno colori, inseriscili nel mlst
            if (edgeWithMinColors.get(0).getColori().isEmpty()) {

                for (int i : indexOfEdgeWithMinColors) {
                    Arco originale = grafo.getArco(i);
                    gestoreMlst.addArcoSenzaInserireCicli(i, originale);

                    //In questo modo, lavoriamo con complessità O(1)
                    tmpArchi.set(i, null);
                }

                rimuoviArchiCheGeneranoCicli(tmpArchi, tmpColori, mlst);

            } else {
                //Determino il colore più ricorrente in edgeWithMinColors
                //Estrazione colori più ricorrenti solo dagli archi con numero colori minimo
                int mostCommonColor = mostCommonColor(edgeWithMinColors);

                //Elimino il colore dagli archi (temporanei)
                rimuoviColore(tmpArchi, tmpColori, mostCommonColor);
            }
        }

        sol = mlst.getListaColori().size();
        return sol;
    }

    private ArrayList<Integer> getIndexOfEdgesWithMinNumberOfColors(ArrayList<Arco> pEdges) {
        ArrayList<Integer> indexOfEdgesWithMinColors = new ArrayList<>();
        int previousTotColorEdge = -1;
        int totColorForEdge = -1;

        for (int i = 0; i < pEdges.size(); i++) {
            if (pEdges.get(i) != null) {
                totColorForEdge = pEdges.get(i).getColori().size();
                //Primo arco analizzato
                if (previousTotColorEdge == -1) {
                    previousTotColorEdge = totColorForEdge;
                    indexOfEdgesWithMinColors.add(i);
                } else //Successivi
                {
                    if (totColorForEdge < previousTotColorEdge) {
                        previousTotColorEdge = totColorForEdge;
                        //Svuoto la lista degli archi minori,
                        //poichè ho trovato un nuovo candidato
                        indexOfEdgesWithMinColors.clear();
                        indexOfEdgesWithMinColors.add(i);
                    } else if (previousTotColorEdge == totColorForEdge) {
                        indexOfEdgesWithMinColors.add(i);
                    }
                }
            }
        }
        return indexOfEdgesWithMinColors;
    }

    private ArrayList<Arco> getEdgesWithMinNumberOfColors(ArrayList<Arco> pEdges, ArrayList<Integer> pIndexOfEdge) {
        ArrayList<Arco> pArchiConColoriMinimi = new ArrayList<>();

        for (int i : pIndexOfEdge) {
            pArchiConColoriMinimi.add(pEdges.get(i));
        }

        return pArchiConColoriMinimi;
    }

    private int mostCommonColor(ArrayList<Arco> pEdges) {
        int mostCommonColor = -1;
        int ricorrenzaMaggiore = -1;

        int[] ricorrenzeColori = new int[this.grafo.getColori().size()];

        //Scorro tutti gli archi e memorizzo le ricorrenze
        //Compessità: O(m * 3)      m -> num. archi, 3 -> num colori per arco
        for (Arco a : pEdges) {
            for (int indiceColore : a.getColori()) {
                ++ricorrenzeColori[indiceColore];
            }
        }

        //Trovo il colore più ricorrente
        //Complessità: O(c)         c -> num. colori
        /*for (int i = 0; i < ricorrenzeColori.length; i++) {
            if (ricorrenzeColori[i] > ricorrenzaMaggiore) {
                ricorrenzaMaggiore = ricorrenzeColori[i];
                mostCommonColor = i;
            }
        }*/
        ArrayList<Integer> coloriPiuComuni = new ArrayList();
        for (int i = 0; i < ricorrenzeColori.length; i++) {
            if (ricorrenzeColori[i] > ricorrenzaMaggiore) {
                coloriPiuComuni.clear();
                coloriPiuComuni.add(i);
                ricorrenzaMaggiore = ricorrenzeColori[i];
                mostCommonColor = i;
            } else if (ricorrenzeColori[i] == ricorrenzaMaggiore) {
                coloriPiuComuni.add(i);
            }
        }

        //RANDOM SHUFFLE
        long seed = System.nanoTime();
        Collections.shuffle(coloriPiuComuni, new Random(seed));

        //return mostCommonColor;
        return coloriPiuComuni.get(0);
    }

    public void rimuoviColore(ArrayList<Arco> archi, ArrayList<Colore> colori, int pColore) {
        //Determino gli indici degli archi in cui è presente pColore
        for (int i : colori.get(pColore).getIndiciArchiCollegati()) {
            archi.get(i).rimuoviColore(pColore);
        }

        //Elimino ogni riferimento di pColore da ogni arco associato
        colori.get(pColore).getIndiciArchiCollegati().clear();
    }

    private void rimuoviArchiCheGeneranoCicli(ArrayList<Arco> archi, ArrayList<Colore> colori, Grafo mlst) {
        int componenteDiRiferimentoNodo1 = 0;
        int componenteDiRiferimentoNodo2 = 0;

        for (int i = 0; i < archi.size(); i++) {
            if (archi.get(i) != null) {
                componenteDiRiferimentoNodo1 = mlst.getNodo(archi.get(i).getDa().getChiave()).getComponenteDiRiferimento();
                componenteDiRiferimentoNodo2 = mlst.getNodo(archi.get(i).getA().getChiave()).getComponenteDiRiferimento();

                //Se l'arco genera cicli
                if (componenteDiRiferimentoNodo1 == componenteDiRiferimentoNodo2) {
                    //Rimuovo l'indice dell'arco dai colori associati
                    for (int colore : archi.get(i).getColori()) {
                        colori.get(colore).rimuoviIndiceArcoCollegato(i);
                    }
                    archi.set(i, null);
                }
            }

        }
    }

    private GrafoColorato generaGrafoSenzaCicli(GrafoColorato mlst) {
        ArrayList<Integer> listaNodiDiPartenza = new ArrayList<>();
        for (int i = 0; i < mlst.dimensione(); i++) {
            listaNodiDiPartenza.add(-1);
        }

        ArrayList<Arco> listaArchiCandidati = new ArrayList<>();
        boolean[] visitato = new boolean[mlst.dimensione()];

        Queue<Nodo> coda = new LinkedList();
        coda.add(mlst.getNodo(0));

        while (!coda.isEmpty()) {
            Nodo nodo = coda.poll();

            if (!visitato[nodo.getChiave()]) {
                visitato[nodo.getChiave()] = true;

                if (nodo.getChiave() != 0) {
                    Arco tmp = mlst.getArco(nodo.getChiave(), listaNodiDiPartenza.get(nodo.getChiave()));
                    listaArchiCandidati.add(tmp);
                }

                for (Nodo adiacente : nodo.getAdiacenti()) {
                    if (!visitato[adiacente.getChiave()]) {
                        coda.add(adiacente);
                        listaNodiDiPartenza.set(adiacente.getChiave(), nodo.getChiave());
                    }
                }
            }
        }

        ArrayList<Nodo> nodiMlst = new ArrayList<>();
        for (int i = 0; i < mlst.dimensione(); i++) {
            nodiMlst.add(new Nodo(i));
        }

        GrafoColorato mlstSenzaCicli = new GrafoColorato(nodiMlst, mlst.getColori().size());

        int index = 0;
        for (Arco arco : listaArchiCandidati) {
            mlstSenzaCicli.addArco(index++, arco);
        }

        return mlstSenzaCicli;
    }
}

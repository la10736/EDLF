# EDLF *Event Driven Logic Framework*

Framework per creare logiche a eventi. Non si tratta di sempici automi a stati finiti, infatti gli stati
di ingresso sono eventi che cambiano gli stati di nodi di input generando un nuovo stato di alcuni nodi
di output. Ovviamente possono essere sempre riscritti come automi a stati finiti, ma la loro definizione 
sarebbe troppo complessa.

Inoltre ogni sottostato può essere un evento di uscita.

## Applicazioni e Esempio Base

L'applicazione immediata sono le porte logiche e un esempio semplice per capire come funziona e questo
semplice circuito:

- A input
- B ouput di XOr tra A e not(A)

Vogliamo postare eventi di cambio di stato di A, ma B non deve cambiare mai. Ovviamente questo semplice 
esempio è riducibile banalmente in un automa triviale, ma in generale cercare di ridurre la logica degli 
automi potrebbe complicare enormemente gli stati e rendere l'algoritmo incompresibile, mentre mantenere
la logica asciutta e semplice anche se apparentemente ridondante potrebbe portare a strutture chiare e 
facilmente modificabili.


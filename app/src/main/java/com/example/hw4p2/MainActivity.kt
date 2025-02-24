package com.example.hw4p2

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainGameScreen()
        }
    }
}

data class GameState(
    val displayedWord: String,
    val guessedLetters: Set<Char> = emptySet(),
    val wrongGuess: Int = 0,
    val numHints: Int = 0,
    val hintMssg: String = ""
)

val wordList = listOf(
    "RIZZLER" to "someone with a lot of charisma/charm",
    "GYATT" to "in the saying: level 10 _____!",
    "NONCHALANT" to "________ dread head",
    "AURA" to "an emenating vibe"
)


//match drawables/imgs to num guesses
@Composable
fun HangmanImg(wrongGuess: Int){
    val imageRes = when (wrongGuess) {
        0 -> R.drawable.begin
        1 -> R.drawable.guess1
        2 -> R.drawable.guess2
        3 -> R.drawable.guess3
        4 -> R.drawable.guess4
        5 -> R.drawable.guess5
        else -> R.drawable.end
    }
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = "Current Hangman Progress",
        modifier = Modifier.size(200.dp)
    )
}

//displays the word/ how many letters guessed
@Composable
fun WordDisplay(word: String, guessedLetters: Set<Char>){
    Row(modifier = Modifier.padding(15.dp)) {
        word.forEach { letter ->
            Text(
                text = if (letter in guessedLetters) letter.toString() else "_",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

//Letter bank display
@Composable
fun LetterBank(guessedLetters: Set<Char>, onLetterClick: (Char) -> Unit) {
    val alphabet = 'A'..'Z'
    Column {
        alphabet.chunked(5).forEach { row ->
            Row {
                row.forEach { letter ->
                    Button(
                        onClick = { onLetterClick(letter) },
                        enabled = letter !in guessedLetters,
                        modifier = Modifier.padding(4.dp)
                    ){
                        Text(letter.toString())
                    }
                }
            }
        }
    }
}

//Hint button + functionality
@Composable
fun HintButton(
    gameState: GameState,
    onHintUsed: () -> Unit
){
    Button(
        onClick = { onHintUsed() },
        enabled = gameState.numHints < 3 && gameState.wrongGuess < 6,
        modifier = Modifier.padding(8.dp)
    ){
        Text(
            text = when (gameState.numHints) {
                0 -> "Show Hint"
                1 -> "Show Hint"
                2 -> "Show Hint"
                else -> "Hint Not Available"
            }
        )
    }
}
//updates the game state when a letter is guessed
fun guessLogic(state: GameState, letter: Char): GameState{
    return if (letter in state.displayedWord) {
        state.copy(guessedLetters = state.guessedLetters + letter)
    }else{
        state.copy(guessedLetters = state.guessedLetters + letter, wrongGuess = state.wrongGuess + 1)
    }
}

//updates state when hint button is clicked: must display word hint, then remove half of remaining letters, then show all vowels
fun hintLogic(state: GameState): GameState {
    return when (state.numHints) {
        0 -> {
            val hintText =
                wordList.find { it.first == state.displayedWord }?.second ?: "No hint available"
            state.copy(numHints = 1, hintMssg = hintText)
        }
        1 -> {
            val allremaining = ('A'..'Z').toSet() - state.displayedWord.toSet() - state.guessedLetters
            val minusHalf = allremaining.shuffled().take(allremaining.size / 2)
            state.copy(
                guessedLetters = state.guessedLetters + minusHalf,
                wrongGuess = state.wrongGuess + 1,
                numHints = 2,
                hintMssg = "Half of the unused letters are removed."
            )
        }
        2 -> {
            val vowels = setOf('A', 'E', 'I', 'O', 'U')
            state.copy(
                guessedLetters = state.guessedLetters + vowels,
                wrongGuess = state.wrongGuess + 1,
                numHints = 3,
                hintMssg = "All vowels are revealed!"
            )
        }
        else -> state.copy(hintMssg = "No more hints available.")
    }
}

//Main screen: Handle memory between orientations, support for both orientations, + style home page
@Composable
fun MainGameScreen() {
    val displayedWord = rememberSaveable { mutableStateOf(newGame().displayedWord) }
    val guessedLetters = rememberSaveable { mutableStateOf(emptySet<Char>()) }
    val wrongGuess = rememberSaveable { mutableStateOf(0) }
    val numHints = rememberSaveable { mutableStateOf(0) }
    val hintMssg = rememberSaveable { mutableStateOf("") }
    //manually saving gameState, rememberSaveable not working for GameState class
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val gameState = GameState(
        displayedWord = displayedWord.value,
        guessedLetters = guessedLetters.value,
        wrongGuess = wrongGuess.value,
        numHints = numHints.value,
        hintMssg = hintMssg.value
    )
    //updates GameState variables manually
    fun updateGameState(newState: GameState) {
        displayedWord.value = newState.displayedWord
        guessedLetters.value = newState.guessedLetters
        wrongGuess.value = newState.wrongGuess
        numHints.value = newState.numHints
        hintMssg.value = newState.hintMssg
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TikTok Brainrot Hangman", fontSize = 24.sp, modifier = Modifier.padding(15.dp))

        if (isLandscape) {
            //all 3 panes must be visible
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    LetterBank(gameState.guessedLetters) { letter ->
                        updateGameState(guessLogic(gameState, letter))
                    }
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    HangmanImg(gameState.wrongGuess)
                    WordDisplay(gameState.displayedWord, gameState.guessedLetters)
                    Row(
                        modifier = Modifier.padding(top = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ){
                        HintButton(gameState) {
                            updateGameState(hintLogic(gameState))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(gameState.hintMssg, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            val newGameState = newGame()
                            updateGameState(newGameState)
                        }) {
                            Text("New Game",style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }else{
            //display vertical
            Column(horizontalAlignment = Alignment.CenterHorizontally){
                HangmanImg(gameState.wrongGuess)
                WordDisplay(gameState.displayedWord, gameState.guessedLetters)
                LetterBank(gameState.guessedLetters) { letter ->
                    updateGameState(guessLogic(gameState, letter))
                }
                HintButton(gameState) {
                    updateGameState(hintLogic(gameState))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(gameState.hintMssg, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = {
                    val newGameState = newGame()
                    updateGameState(newGameState)
                }) {
                    Text("New Game")
                }
            }
        }
        LaunchedEffect(gameState.wrongGuess, gameState.guessedLetters) {
            if (gameState.wrongGuess >= 6){
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("You lost! The word was: ${gameState.displayedWord}")
                }
            }else if (gameState.displayedWord.all { it in gameState.guessedLetters }) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("You Won! Great Job!")
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

//start new game
fun newGame(): GameState {
    val (word, _) = wordList.random()
    return GameState(displayedWord = word)
}




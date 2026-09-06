package com.bingo.controller;

import com.bingo.dto.BingoDtos.*;
import com.bingo.model.BingoGrid;
import com.bingo.model.GameInstance;
import com.bingo.service.GameService;
import com.bingo.service.PdfService;
import com.bingo.service.WordService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * REST controller exposing all Bingo game endpoints.
 *
 * Base path: /api/games
 * Also handles: POST /api/random-words
 */
@RestController
@CrossOrigin(origins = "*")
public class GameController {

    private final GameService gameService;
    private final PdfService  pdfService;
    private final WordService  wordService;

    public GameController(GameService gameService, PdfService pdfService, WordService wordService) {
        this.gameService = gameService;
        this.pdfService  = pdfService;
        this.wordService = wordService;
    }

    // ── Random word generation (pre-game) ─────────────────────────

    /**
     * POST /api/random-words
     * Body: { count: N, excludeWords: [...] }
     * Returns N random dictionary words not in excludeWords.
     */
    @PostMapping("/api/random-words")
    public ResponseEntity<?> randomWords(@RequestBody RandomWordsRequest request) {
        try {
            List<String> words = wordService.randomWords(
                    request.getCount(),
                    request.getExcludeWords() == null ? List.of() : request.getExcludeWords());
            return ResponseEntity.ok(new RandomWordsResponse(words));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── Create game ───────────────────────────────────────────────

    /**
     * POST /api/games
     * Body: { words: [...], playerCount: N }
     */
    @PostMapping("/api/games")
    public ResponseEntity<?> createGame(@RequestBody CreateGameRequest request) {
        try {
            GameInstance game = gameService.createGame(request.getWords(), request.getPlayerCount());
            return ResponseEntity.ok(new CreateGameResponse(game.getId(), game.getGrids().size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── Add grids ─────────────────────────────────────────────────

    /**
     * POST /api/games/{id}/add-grids
     * Body: { playerCount: N }
     * Generates N additional grids for an existing game still in SETUP state.
     * Returns the index of the first new grid so the client can download just those.
     */
    @PostMapping("/api/games/{id}/add-grids")
    public ResponseEntity<?> addGrids(@PathVariable String id,
                                       @RequestBody AddGridsRequest request) {
        try {
            int fromIndex = gameService.addGrids(id, request.getPlayerCount());
            int total     = gameService.getGame(id).map(g -> g.getGrids().size()).orElse(0);
            int newCount  = total - fromIndex;
            return ResponseEntity.ok(new AddGridsResponse(newCount, total, fromIndex));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── Game state ────────────────────────────────────────────────

    @GetMapping("/api/games/{id}/state")
    public ResponseEntity<?> getState(@PathVariable String id) {
        return gameService.getGame(id)
                .map(game -> {
                    GameStateResponse resp = new GameStateResponse();
                    resp.setGameId(game.getId());
                    resp.setState(game.getState());
                    resp.setCalledWords(gameService.getCalledWordsSorted(id));
                    resp.setRemainingCount(game.getRemainingWords().size());
                    resp.setGrids(game.getGrids());
                    if (!game.getCalledWords().isEmpty()) {
                        resp.setCurrentWord(game.getCalledWords().get(game.getCalledWords().size() - 1));
                    }
                    return ResponseEntity.ok((Object) resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Start game ────────────────────────────────────────────────

    @PostMapping("/api/games/{id}/start")
    public ResponseEntity<?> startGame(@PathVariable String id) {
        try {
            GameInstance game = gameService.startGame(id);
            GameStateResponse resp = new GameStateResponse();
            resp.setGameId(game.getId());
            resp.setState(game.getState());
            resp.setCalledWords(List.of());
            resp.setRemainingCount(game.getRemainingWords().size());
            resp.setGrids(game.getGrids());
            return ResponseEntity.ok(resp);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── Next word ─────────────────────────────────────────────────

    @PostMapping("/api/games/{id}/next-word")
    public ResponseEntity<?> nextWord(@PathVariable String id) {
        try {
            String word = gameService.nextWord(id);
            List<String> called = gameService.getCalledWordsSorted(id);
            int remaining = gameService.getGame(id)
                    .map(g -> g.getRemainingWords().size())
                    .orElse(0);
            return ResponseEntity.ok(new NextWordResponse(word, called, remaining));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── End game ──────────────────────────────────────────────────

    @PostMapping("/api/games/{id}/end")
    public ResponseEntity<?> endGame(@PathVariable String id) {
        try {
            GameInstance game = gameService.endGame(id);
            GameStateResponse resp = new GameStateResponse();
            resp.setGameId(game.getId());
            resp.setState(game.getState());
            resp.setCalledWords(gameService.getCalledWordsSorted(id));
            resp.setRemainingCount(0);
            return ResponseEntity.ok(resp);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Download PDFs ─────────────────────────────────────────────

    /**
     * GET /api/games/{id}/download-grids?from=0
     * Generates one PDF per grid starting at index `from` (default 0 = all grids).
     * The filename includes the range so successive downloads have distinct names.
     */
    @GetMapping("/api/games/{id}/download-grids")
    public ResponseEntity<?> downloadGrids(@PathVariable String id,
                                            @RequestParam(defaultValue = "0") int from) {
        return gameService.getGame(id).map(game -> {
            try {
                List<BingoGrid> grids = game.getGrids();
                List<BingoGrid> subset = grids.subList(
                        Math.max(0, Math.min(from, grids.size())),
                        grids.size());

                ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
                try (ZipOutputStream zos = new ZipOutputStream(zipBuffer)) {
                    for (BingoGrid grid : subset) {
                        byte[] pdf = pdfService.generateGridPdf(grid, "Bingo");
                        String fileName = "player_" + grid.getPlayerNumber() + "_grid.pdf";
                        zos.putNextEntry(new ZipEntry(fileName));
                        zos.write(pdf);
                        zos.closeEntry();
                    }
                }

                // Filename encodes the player range for clarity
                int first = subset.isEmpty() ? from + 1 : subset.get(0).getPlayerNumber();
                int last  = subset.isEmpty() ? from + 1 : subset.get(subset.size() - 1).getPlayerNumber();
                String zipName = (first == last)
                        ? "bingo_grids_player_" + first + ".zip"
                        : "bingo_grids_players_" + first + "-" + last + ".zip";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/zip"));
                headers.setContentDispositionFormData("attachment", zipName);

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(zipBuffer.toByteArray());

            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(("PDF generation failed: " + e.getMessage()).getBytes());
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}

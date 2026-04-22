package app.musiceditorclient.models;

public record RecursiveClipDialogResult(
       float startingSeconds,
       float secondsBetweenRepetition,
       int numberOfRepetitions
) {}

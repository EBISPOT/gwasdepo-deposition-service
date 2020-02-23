package uk.ac.ebi.spot.gwas.deposition.rest.dto;


import uk.ac.ebi.spot.gwas.deposition.domain.Note;
import uk.ac.ebi.spot.gwas.deposition.dto.NoteDto;

public class NoteDtoAssembler {

    public static NoteDto assemble(Note note) {
        return new NoteDto(note.getStudyTag(),
                note.getNote(),
                note.getNoteSubject(),
                note.getStatus());
    }

    public static Note disassemble(NoteDto noteDto) {
        Note note = new Note();
        note.setStudyTag(noteDto.getStudyTag());
        note.setStatus(noteDto.getStatus());
        note.setNoteSubject(noteDto.getNoteSubject());
        note.setNote(noteDto.getNote());

        return note;
    }
}

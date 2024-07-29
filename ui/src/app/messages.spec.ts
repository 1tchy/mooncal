import {Messages} from "./messages";
import messagesDE from "./messages.de.json";
import messagesEN from "./messages.en.json";
import messagesNL from "./messages.nl.json";
import messagesES from "./messages.es.json";
import messagesFR from "./messages.fr.json";
import messagesRO from "./messages.ro.json";

describe('Messages', () => {
  let allMessages: Messages[] = [messagesDE, messagesEN, messagesNL, messagesES, messagesFR, messagesRO];

  it('should have different lang.curr', () => {
    let langIds = new Set(allMessages.map(message => message.lang.current));
    expect(langIds.size).toBe(allMessages.length);
  });
});

# conformance
Import and persist conformed data to replacement.

This is where we tackle the long tail problem of how to ensure that as much data as possible can be imported to replacement in a meaningful form.

By meaningful we mean that it conforms to a spec and that the conformer of that spec will provide data that can be round-tripped.

The flow is like this:

```mermaid
graph TD
    A[Text Source]-->|Text| B(Save);
    B --> C{Replacement};
    C -->|Text| D(fa:fa-book-open read);
    D -->|EDN Clojure code| E(fa:fa-check conform);
    E -->|EDN data| F[fa:fa-hashtag digest];
    F -->|EDN data| I[fa:fa-database store];
    E -->|EDN data| G(fa:fa-recycle unform);
    G -->|EDN Clojure code| H(fa:fa-pen print);
    H -->|Text| C;
```

If the text source can listen, it will be given back the same text as input with the assurance that it conformed and was persisted.
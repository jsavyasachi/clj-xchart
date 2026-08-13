# Contributing to clj-xchart

Thanks for your interest in `clj-xchart`. Bug reports, fixes, and focused
feature contributions are all welcome.

## Before you start

- For more than a small fix, **open an issue first**. Then we can agree on the
  approach before you spend time.
- Read the existing issues and pull requests. This prevents duplicate work.

## Development

This is a Clojure library. You need a JDK and [Leiningen](https://leiningen.org/).
Projects that moved to `deps.edn` use the Clojure CLI instead. See the README.

```bash
lein test     # run the test suite
lein check    # AOT-compile; must be free of reflection warnings
```

A change must meet these conditions before it can merge:

- **Tests first.** Add or update tests for the behavior you change. For a bug
  fix, include a regression test that fails before your fix and passes after.
- **Green build.** `lein test` passes and `lein check` reports **zero**
  reflection warnings.
- **One change.** Keep each pull request to one logical change.

## Commits and pull requests

- Follow [Conventional Commits](https://www.conventionalcommits.org/)
  (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:` …).
- Keep the subject in the imperative mood and under ~72 characters.
- Update `CHANGELOG.md` when your change is user-visible.
- Rebase on the latest `main` before opening the pull request.

## License

If you contribute, you agree to license your contributions under the same
license as this project. See `LICENSE` and the README.

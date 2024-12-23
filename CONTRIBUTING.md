<!-- omit in toc -->

# Contributing to ScanBridge

Thanks for your interest in contributing to ScanBridge! ❤️

All types of contributions are encouraged and valued. See
the [Table of Contents](#table-of-contents) for different ways to help and details about how this
project handles them. Please make sure to read the relevant section before making your contribution.
It will make it a lot easier for us maintainers and smooth out the experience for all involved. The
community looks forward to your contributions. 🎉

> And if you like the project, but just don't have time to contribute, that's fine. There are other
> easy ways to support the project and show your appreciation, which we would also be very happy
> about:
> - Star the project
> - Tweet about it
> - Refer this project in your project's readme
> - Mention the project at local meetups and tell your friends/colleagues

<!-- omit in toc -->

## Table of Contents

- [I Have a Question](#i-have-a-question)
    - [I Want To Contribute](#i-want-to-contribute)
    - [Reporting Bugs](#reporting-bugs)
    - [Suggesting Enhancements](#suggesting-enhancements)
    - [Your First Code Contribution](#your-first-code-contribution)
    - [Contributing translations](#contributing-translations)
- [Styleguides](#styleguides)
    - [Commit Messages](#commit-messages)
- [Join The Project Team](#join-the-project-team)

## Code of Conduct

Please just be respectful, patient and considerate of others. This code applies to all spaces
managed by the project.
If you encounter any unacceptable behavior, please report it to <Chrisimx>.

## I Have a Question

> If you want to ask a question, we assume that you have read the available [Documentation]().

Before you ask a question, it is best to search for
existing [Issues](https://github.com/Chrisimx/ScanBridge/issues)
or [Discussions](https://github.com/Chrisimx/ScanBridge/discussions) that might help you. In case
you have found a suitable issue and still need clarification, you can write your question in this
issue.

If you then still feel the need to ask a question and need clarification, we recommend the
following:

- Open an [Issue](https://github.com/Chrisimx/ScanBridge/issues/new)
  or [Discussion](https://github.com/Chrisimx/ScanBridge/discussion/new), based on whether the
  problem is a bug or feature request or a general question.
- Provide as much context as you can about what you're running into.
- Provide project and platform versions (Android version, ScanBridge version), depending on what
  seems relevant.

We will then take care of the issue as soon as possible.

<!--
You might want to create a separate issue tag for questions and include it in this description. People should then tag their issues accordingly.

Depending on how large the project is, you may want to outsource the questioning, e.g. to Stack Overflow or Gitter. You may add additional contact and information possibilities:
- IRC
- Slack
- Gitter
- Stack Overflow tag
- Blog
- FAQ
- Roadmap
- E-Mail List
- Forum
-->

## I Want To Contribute

> ### Legal Notice <!-- omit in toc -->
> When contributing to this project, you must agree that you have authored 100% of the content, that
> you have the necessary rights to the content and that the content you contribute may be provided
> under the project licence.

### Reporting Bugs

<!-- omit in toc -->

#### Before Submitting a Bug Report

A good bug report shouldn't leave others needing to chase you up for more information. Therefore, we
ask you to investigate carefully, collect information (like the crash report given by ScanBridge
after the app crashed) and describe the issue in detail in your report. Please complete the
following steps in advance to help us fix any potential bug as fast as possible.

- Make sure that you are using the latest version.
- Determine if your bug is really a bug and not an error on your side e.g. using incompatible
  environment components/versions (Make sure that you have read the [documentation](). If you are
  looking for support, you might want to check [this section](#i-have-a-question)).
- To see if other users have experienced (and potentially already solved) the same issue you are
  having, check if there is not already a bug report existing for your bug or error in
  the [issue tracker](https://github.com/Chrisimx/ScanBridge/issues).
- Collect information about the bug:
    - Stack trace (Traceback) using the Android logcat
    - If it's a crash ScanBridge will provide you with the exception on the next start. Please
      provide this in your bug report
    - Android version and ScanBridge version as given by the settings in the app
    - Information about your scanner
    - Possibly your input and the output
    - Can you reliably reproduce the issue? And can you also reproduce it with older versions?

<!-- omit in toc -->

#### How Do I Submit a Good Bug Report?

> You must never report security related issues, vulnerabilities or bugs including sensitive
> information to the issue tracker, or elsewhere in public. Instead sensitive bugs must be sent by
> email to <chris.imx@online.de>.
<!-- You may add a PGP key to allow the messages to be sent encrypted as well. -->

We use GitHub issues to track bugs and errors. If you run into an issue with the project:

- Open an [Issue](https://github.com/Chrisimx/ScanBridge/issues/new). Choose between the issue
  templates.
- Explain the behavior you would expect and the actual behavior.
- Please provide as much context as possible and describe the *reproduction steps* that someone else
  can follow to recreate the issue on their own.
- Provide the information you collected in the previous section.

Once it's filed:

- The project team will label the issue accordingly.
- A team member will try to reproduce the issue with your provided steps. If there are no
  reproduction steps or no obvious way to reproduce the issue, the team will ask you for those steps
  and mark the issue as `needs-repro`. Bugs with the `needs-repro` tag will not be addressed until
  they are reproduced.
- If the team is able to reproduce the issue, it will be marked `needs-fix`, as well as possibly
  other tags (such as `critical`), and the issue will be left to
  be [implemented by someone](#your-first-code-contribution).

<!-- You might want to create an issue template for bugs and errors that can be used as a guide and that defines the structure of the information to be included. If you do so, reference it here in the description. -->

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for ScanBridge, **including
completely new features and minor improvements to existing functionality**. Following these
guidelines will help maintainers and the community to understand your suggestion and find related
suggestions.

<!-- omit in toc -->

#### Before Submitting an Enhancement

- Make sure that you are using the latest version.
- Perform a [search](https://github.com/Chrisimx/ScanBridge/issues) to see if the enhancement has
  already been suggested. If it has, add a comment to the existing issue instead of opening a new
  one.
- Find out whether your idea fits with the scope and aims of the project. It's up to you to make a
  strong case to convince the project's developers of the merits of this feature.

<!-- omit in toc -->

#### How Do I Submit a Good Enhancement Suggestion?

Enhancement suggestions are tracked
as [GitHub issues](https://github.com/Chrisimx/ScanBridge/issues).

- Use a **clear and descriptive title** for the issue to identify the suggestion.
- Provide a **step-by-step description of the suggested enhancement** in as many details as
  possible.
- **Describe the current behavior** and **explain which behavior you expected to see instead** and
  why. At this point you can also tell which alternatives do not work for you.
- You may want to **include screenshots or screen recordings** which help you demonstrate the steps
  or point out the part which the suggestion is related to. You can
  use [LICEcap](https://www.cockos.com/licecap/) to record GIFs on macOS and Windows, and the
  built-in [screen recorder in GNOME](https://help.gnome.org/users/gnome-help/stable/screen-shot-record.html.en)
  or [SimpleScreenRecorder](https://github.com/MaartenBaert/ssr) on
  Linux. <!-- this should only be included if the project has a GUI -->
- **Explain why this enhancement would be useful** to most ScanBridge users. You may also want to
  point out the other projects that solved it better and which could serve as inspiration.

### Your First Code Contribution

<!-- TODO
include Setup of env, IDE and typical getting started instructions?

-->
All code contributions happen on the GitHub repository of ScanBridge. You probably already have
found something that you want to work on. If not, you can take a look at the current issues. Before
beginning to work on the fix of the bug/the feature leave a notice in the corresponding issue that
you want to work on it (if there is no issue yet, create one). This way no work gets done twice, you
get feedback and maintainers can evaluate if your solution works for everybody.

The process for contributing code changes works as follows:

1. Fork the main ScanBridge repository and create commits on your own feature branch.
2. Submit a pull request (PR).
3. Wait for maintainers to review the PR and address the relevant feedback.
4. After receiving maintainer approval, the PR is merged after a short grace period.

In the best case, your commits should be atomic, meaning that they should all compile and work on
their own. This way, it is easier for the maintainers to review your code and to merge it.
Also, your commits should ideally be GPG signed. This way, it is easier to verify that you are the
author of the commit. You can find more information on how to sign your commits
in the [Git book](https://git-scm.com/book/en/v2/Git-Tools-Signing-Your-Work).

To make a code change, you'll need to first fork the repository on GitHub, if
you haven't already. To do so, click the "Fork" button in the top right corner
of the repository's web page.

We assume that you are already familiar with Git, which we use to track changes.
An excellent resource for Git is the _Pro Git_ book by Scott Chacon and Ben
Straub, which is available online for free
[here](https://git-scm.com/book/en/v2).

The recommended IDE, which is also used by the maintainers, is Android Studio. Simply open the
cloned repository in it.
This should allow you to easily compile ScanBridge and load it using the emulator or a device. If
you want to use another
IDE take notice that Gradle is used as the build system.

### Contributing translations

If you want to contribute translations, you can do so by translating the strings.xml file in the
`res/values` folder.
You can also add a new folder for your language if it does not exist yet. The folder name should be
the language code, e.g. `values-de` for German.
You can then add the translated strings.xml file to this folder. Use the translated German
strings.xml file as a reference for the structure of the file.
If you are unsure about the translation, you can also ask in the issue tracker or in the
discussions.

## Styleguides

### Commit Messages

Keep your commit messages short. You can put further explanations in the body of commit message,
seperated by a paragraph. Be sure to use the imperative form of the verb, e.g. "Add README.md" and
not "Added README.md".

## Join The Project Team

For becoming a project team member you should already have shown that you can provide good
contributions, e.g. in pull requests to ScanBridge or eSCLKt and in the best case also have
experience with open-source work in general. If you then decide that you want to become part of the
project team, you are welcome to send an application e-mail to <chris.imx@online.de>.

<!-- omit in toc -->

## Attribution

This guide is based on the [contributing.md](https://contributing.md/generator)!

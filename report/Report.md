# Software Testing -- PDFBox

----

## 1. Introduction

The PDFBox library is an open source Java tool developed by Apache PDFBox community
for working with PDF documents. This project allows creation
of new PDF documents, manipulation of existing documents and the ability
to extract content from documents. Apache PDFBox also includes several
command-line utilities. In this project, I will do some
systematic functional testing and partition testing on
the key features and functions of PDFBox.

## 2. System Overview

The main features of PDFBox includes Extract Text ,Split & Merge,
Fill Forms ,Preflight ,Print ,Save as Image ,Create PDFs and Signing.

### 2.1 Architecture and Modules

Apache PDFBox is organized as a multi-module Maven project.
The `pdfbox` module provides the core functionality for parsing
and manipulating PDF documents and serves as the primary subject
of testing in this report. Supporting modules such as `fontbox`
and `xmpbox` handle font processing and metadata management,
respectively. Additional modules including `tools` offer command-line
utilities built on top of the core library, while example, benchmarking,
and debugging modules are excluded from the testing scope.

```mermaid
---
title:  Architecture of Core Modules
---
flowchart
    PDFBox --> m1("fontbox") & m2("io") & m3("pdfbox") & m4("tools") & m5("xmpbox")
    m3 --> n1("filter") & n2("pdfparser") & n3("pdfwriter") & n4("printing") & n5("...")
    m4 --> n8("CLI tools of pdfbox api")
```

### 2.2 Technology Stack

Apache PDFBox is implemented primarily in Java(more than 99%) and is organized
as a multi-module Maven project. The build and dependency management
are handled through Maven, with a dedicated parent
module (`pdfbox-parent`) defining shared configurations
and dependency versions across all submodules.

The project uses JUnit 5 (`JUnit.Jupiter`) as its testing framework.
All major functional modules, including pdfbox, fontbox, io, xmpbox,
and tools, declare dependencies on `org.junit.jupiter:junit-jupiter`.
This consistent testing setup enables a uniform approach to writing
and executing unit tests across modules.

### 2.3 Project Size and Structure

Based on a repository-wide code analysis using `cloc`,
the project contains over 4,000 source files and approximately
410,000 lines of code, including comments and blank lines.

The implementation is predominantly written in Java, which accounts
for approximately 168,000 lines of executable code, in addition
to supporting resources such as XML configuration files, HTML documentation,
and build metadata. The system is structured into multiple functional modules,
with the pdfbox module serving as the core library for PDF parsing and
document manipulation.

Test code is distributed across several modules and follows
Maven’s conventional directory structure (`/src/test/java`), indicating
an established testing practice within the project. Given the
size and modularity of the system, targeted functional testing is
necessary to achieve meaningful coverage without attempting exhaustive
testing of the entire codebase.

### 2.4 Building & Execution

The project was built and tested locally using Maven 3 from
the repository root. A full clean build and test run was executed with:

`mvn clean test`

The build completed successfully for the entire multi-module
reactor (11 modules) on 2026-02-01. During the build,
Maven Enforcer verified the Java and Maven environment requirements,
Checkstyle was executed with 0 violations.

### 2.5 Existing Test Cases Analysis

PDFBox contains an established automated test suite distributed
across multiple Maven modules under the conventional `src/test/java` layout.
A repository scan indicates 217 Java test classes matching the `*Test*.java` naming pattern.
Test code is primarily concentrated in the core library module
`pdfbox` (122 test classes), with additional coverage in supporting
modules such as `fontbox` (42), `xmpbox` (28), and `io` (10).
Smaller test suites also exist in `tools` (6) and `examples` (9).

The test suite reflects a mix of unit-level and component-level
functional tests. For example, the pdfbox module includes
tests for critical behaviors such as encryption
(`TestPublicKeyEncryption`, `TestSymmetricKeyEncryption`),
as well as utility and correctness checks (`MatrixTest`, `TestHexUtil`, `TestDateUtil`
, `TestNumberFormatUtil`). The presence of tests in
tools (e.g., `TestExtractText`, `TestTextToPdf`, `TestPDFText2HTML`)
indicates validation of command-line wrappers.

The project uses JUnit 5 (JUnit Jupiter) consistently across modules
(managed centrally in the parent configuration). Tests can be
executed from the repository root using Maven 3 (e.g., `mvn clean test`),
which runs the test suites across the full reactor build.
Overall, the distribution and naming conventions suggest a mature testing structure,
while giving chances for additional targeted functional tests
derived from systematic partitioning of inputs for selected features.

## 3 Systematic Functional Testing & Partition Testing

In this project, I chose the Text Extraction as the testing feature.

### 3.1 Motivation for Systematic Functional Testing and Partition Testing

PDF processing software operates on a highly complex and largely
unbounded input space, as PDF documents may vary significantly
in structure, encoding, layout, and validity. Exhaustive testing
of all possible inputs is infeasible. As a result, random or
example-driven testing is insufficient to ensure steady
functional behavior across various possible realistic scenarios.

Systematic functional testing focuses on selecting test inputs
that exercise different observable behaviors, rather than
testing arbitrary examples. Partition testing supports this
by grouping similar inputs together and testing a representative
case from each group, which allows broad functional coverage
with relatively few tests.

This approach is well suited to Apache PDFBox. Although the existing tests
already cover different input conditions.
However, those input categories are not clearly defined and documented.
In this project, partition testing is applied explicitly to the
text extraction functionality to cover both normal and
error-related cases in a controlled and repeatable manner.

### 3.2 Why Text Extraction Was Selected

Text extraction was chosen because it is one of the core
features of PDFBox and is widely used in practice. Even there are
various apps using OCR and AI based technology to do the same work
precise and good. The behavior of text extraction depends
on many properties of the input PDF, such as document structure,
page layout, character encoding, and configuration options.
Different types of PDF files can therefore produce different
observable behaviors.

Text extraction is also easy to test in a controlled way.
Small PDF files can be created or stored as test resources,
and the extracted text can be checked directly. This makes
it suitable for designing clear and repeatable functional
tests, including both normal ca· ses and error cases.

### 3.3 Existing Tests for Text Extraction

PDFBox already contains several tests related to text extraction
defined in `TestTextStripper`. Here is the test cases included:

| Test Case                                                                                          | Test Level / Type                           | Covered Input Partitions                                                                                                        | Limitations                                                                                                      |
|----------------------------------------------------------------------------------------------------|---------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| **Extract**:Run text extraction on many PDF files and compare results with known-good text outputs | System-level regression test (golden files) | - Sorting enabled vs disabled<br>- Different input sources (input vs ext directories)<br>- Batch mode vs single-file debug mode | Coverage depends on available PDFs; comparison is tolerant (whitespace and some Unicode differences are ignored) |
| **StripByOutlineItems**: Verify text extraction using outline bookmarks and page ranges            | Component-level functional test             | - Bookmark-based range vs page-based range<br>- Single-page vs multi-page ranges<br>- Orphan bookmark (expect empty output)     | Assumes specific outline structure; does not cover nested or non-page destinations                               |
| **Tabula**: Validate output of a customized `PDFTextStripper` implementation                       | Regression / strategy-variant test          | - Alternative extraction strategy (overridden font height logic)<br>- Specific document input                                   | Internal branches in `computeFontHeight()` are not explicitly covered or asserted                                |
| **StartEndPage**: Extract text from a single specified page                                        | Component-level functional test             | - Single-page range (start = end)<br>- Fixed input document                                                                     | Does not test multi-page, boundary pages, or invalid page ranges                                                 |
| **IgnoreContentStreamSpaceGlyphs**: Verify behavior of ignoring space glyphs in overlapping text   | Option-specific functional test             | - Ignore space glyphs enabled<br>- Overlapping text layout<br>- Sorting enabled                                                 | Does not compare behavior when the option is disabled; relies on exact string match                              |

### 3.4 Partition Design

Based on the principle of partition design, we partition by input features that can alter
observable behavior and design this scheme.

| ID  | Partition Description                                                       | Key Input Characteristics            | Expected Behavior                                                                    | Covered by Existing Tests                                                                    |
|-----|-----------------------------------------------------------------------------|--------------------------------------|--------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| P1  | Single-page text PDF                                                        | One page, simple ASCII text          | Text is extracted correctly                                                          | Partially (`testExtract`)                                                                    |
| P2  | Multi-page text PDF                                                         | Multiple pages with text             | Text extracted in page order                                                         | Partially (`testExtract`)                                                                    |
| P3  | PDF with Unicode text                                                       | Non-ASCII characters (e.g., Chinese) | Unicode text preserved                                                               | Limited (resource-dependent)                                                                 |
| P4  | Rotated page PDF                                                            | Page rotated (90/270 degrees)        | Text still extracted correctly                                                       | Not explicit                                                                                 |
| P5  | Page range extraction                                                       | Valid start/end page range           | Only selected pages extracted                                                        | Yes (`testStartEndPage`)                                                                     |
| P6  | Outline-based extraction                                                    | Valid outline bookmarks              | Text matches page-range result                                                       | Yes (`testStripByOutlineItems`)                                                              |
| P7  | PDF without extractable text                                                | Image-only or empty content          | Empty or near-empty output                                                           | Not explicit                                                                                 |
| P8  | Encrypted PDF (no permission)                                               | Encrypted, no password               | Extraction fails or throws exception                                                 | Not explicit                                                                                 |
| P9  | `two-columns.pdf` (two-column layout)                                       | New test resource PDF                | Multi-column layout is a classic case where `sortByPosition` changes reading order   | `testExtract()` runs sort on/off, but does not isolate ordering differences in a minimal way |
| P10 | Runtime-generated PDF with invalid page range (start > end / out of bounds) | Generated at runtime                 | Boundary/invalid input: verifies failure mode or defined behavior for invalid ranges | Existing tests only cover one valid single-page range                                        |
| P11 | Encrypted PDF with wrong password                                           | Generated at runtime                 | Different from “no password”: verifies error handling on wrong credentials           | Not explicitly covered in `TestTextStripper`                                                 |

The input space of text extraction is divided into a set of partitions
based on document structure, content type, configuration, and error
conditions. Some of these partitions are already covered by existing
tests, while others are only partially covered or not covered at all.
This partition scheme is used to guide the selection of representative
test cases in the next step.

### 3.5 Representative Inputs I Selected

This section only includes representative inputs that are not already well
covered by the existing test suite. Basic cases such as single-page,
multi-pages, and general Unicode text extraction are excluded,
as they are already exercised by the existing regression test. To make testing
visible, we use pre-generated fixed inputs PDF.

| Partition ID | Representative Input (Selected by Me)                                             | Why This Input Is Representative                                                                                  |
|--------------|-----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| P4           | `rotated-page.pdf` (one page rotated 90° or 270° with simple text)                | A minimal rotated-page PDF isolates rotation as the only variable and checks rotation-related extraction behavior |
| P7           | `image-only.pdf` (image-only, no text objects)                                    | Represents a valid PDF where text extraction should return empty (or near-empty) output                           |
| P8           | Runtime-generated encrypted PDF (no password provided)                            | Represents the failure mode when encrypted content cannot be accessed without credentials                         |
| P9           | `two-columns.pdf` (two columns with clear left-to-right / top-to-bottom ordering) | Isolates a classic reading-order case where `setSortByPosition(true/false)` should produce different text order   |
| P10          | Runtime-generated multi-page PDF + invalid ranges (start>end and out-of-bounds)   | Provides controlled boundary inputs to verify the defined failure/handling behavior of invalid page ranges        |
| P11          | Runtime-generated encrypted PDF + wrong password                                  | Separates “wrong password” from “no password” and verifies consistent error handling for invalid credentials      |

### 3.6 Mapping Inputs to JUnit Tests

| New Test Method                                      | Partition | Test Input PDF                  | Main Assertion (What to Verify)                                      | Notes                                     |
|------------------------------------------------------|-----------|---------------------------------|----------------------------------------------------------------------|-------------------------------------------|
| `testExtractText_rotatedPage()`                      | P4        | `rotated-page-90.pdf`           | Extracted text contains `ROTATED_90` (normalize whitespace)          | Use `contains()`; avoid full string match |
| `testExtractText_imageOnly_returnsEmpty()`           | P7        | `image-only.pdf`                | `extracted.trim()` is empty (or very small)                          | Ensure PDF truly has no text layer        |
| `testExtractText_encrypted_noPassword_throws()`      | P8        | `encrypted-userpass-secret.pdf` | Extraction fails without password (assert throws)                    | Assert exception type if stable           |
| `testExtractText_twoColumns_sortingChangesOrder()`   | P9        | `two-columns.pdf`               | Output differs for sort on/off; sorted output matches expected order | Design obvious c1 then c2 ordering        |
| `testExtractText_invalidRange_startGreaterThanEnd()` | P10       | `multi-page-3.pdf`              | Defined behavior for start>end (throws or empty)                     | Run once to observe, then lock behavior   |
| `testExtractText_invalidRange_outOfBounds()`         | P10       | `multi-page-3.pdf`              | Defined behavior for out-of-bounds (throws or clamp/empty)           | Same approach: observe then lock          |
| `testExtractText_encrypted_wrongPassword_throws()`   | P11       | `encrypted-userpass-secret.pdf` | Extraction fails with wrong password (assert throws)                 | Separate from no-password case            |

Run `mvn -pl pdfbox -Dtest=TestTextExtractionPartitions test
` All Junit Tests passed;

```xml

<testcase name="testExtractText_invalidRange_outOfBounds"
          classname="org.apache.pdfbox.text.TestTextExtractionPartitions" time="0.299"/>
<testcase name="testExtractText_imageOnly_returnsEmpty" classname="org.apache.pdfbox.text.TestTextExtractionPartitions"
          time="0.016"/>
<testcase name="testExtractText_encrypted_noPassword_throws"
          classname="org.apache.pdfbox.text.TestTextExtractionPartitions" time="0.013"/>
<testcase name="testExtractText_invalidRange_startGreaterThanEnd"
          classname="org.apache.pdfbox.text.TestTextExtractionPartitions" time="0.001"/>
<testcase name="testExtractText_encrypted_wrongPassword_throws"
          classname="org.apache.pdfbox.text.TestTextExtractionPartitions" time="0.002"/>
<testcase name="testExtractText_twoColumns_sortingChangesOrder"
          classname="org.apache.pdfbox.text.TestTextExtractionPartitions" time="0.097"/>
<testcase name="testExtractText_rotatedPage" classname="org.apache.pdfbox.text.TestTextExtractionPartitions"
          time="0.437">
-------------------------------------------------------------------------------
Test set: org.apache.pdfbox.text.TestTextExtractionPartitions
-------------------------------------------------------------------------------
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.878 s -- in
org.apache.pdfbox.text.TestTextExtractionPartitions
</testcase>
```

### 3.7 Results and Observations

All newly added JUnit tests passed successfully on the current version of Apache PDFBox.
The results confirm that PDFTextStripper behaves consistently across the selected
input partitions.

For rotated-page PDFs (P4), text extraction succeeds and returns the expected content,
indicating that page rotation does not prevent correct text extraction.

For image-only PDFs (P7), the extractor returns empty or near-empty output,
which matches the expected behavior when no text objects exist.

Encrypted PDFs without a password or with a wrong password (P8, P11) consistently
fail during loading or extraction, demonstrating stable error handling for
protected documents.

For two-column layouts (P9), enabling sortByPosition changes the reading order,
and the sorted output follows the expected left-to-right column order.

Invalid page range inputs (P10) result in empty output, which defines the current behavior of PDFTextStripper for such
boundary conditions.

Overall, the observed behaviors match the expected outcomes defined for each partition.

## 4 Finite Functional Model in SW-Testing

### 4.1 Why Finite Model

A finite model is an abstract way to describe system behavior
using a limited number of states and transitions. In this course,
the most common form is a Finite State Machine (FSM), which represents
states as nodes and transitions as directed edges. Even though a real
program may have many internal configurations, an FSM captures only
the important behavioral states needed for analysis.

Finite models are useful for testing because they make the specification
clearer and more structured. Instead of relying only on natural-language
descriptions, the model explicitly shows:

- What states are allowed
- What transitions are valid
- How the system should react to events

This helps reduce ambiguity and makes expected behavior easier to verify. And
most important one, it helps us organize test cases.

Another advantage is that finite models guide test design.
Each state and each transition can become a test target. By checking whether
all states and transitions have been exercised, we obtain a clear
testing criterion rather than guessing when testing is “enough.”

In our PDFBox project, many components are relatively simple and do
not involve complex state changes. However, some features involve
multiple stages of processing and different behavioral modes. For example,
certain operations depend on whether a document is loaded, modified,
saved, or closed. These situations naturally form distinct states with different
allowed operations. Modeling such features using an FSM makes it easier
to clearly define valid transitions and identify invalid sequences.
This structure helps us systematically derive test cases that cover
meaningful state changes rather than only individual method calls.

### 4.2 Document Lifecycle as an FSM

Many components in PDFBox are implemented as individual API operations
that perform specific tasks without forming complex state-driven behavior.
As a result, not all features naturally lend themselves to finite state modeling.

However, the lifecycle of a PDF document session exhibits clear state-dependent
constraints. Operations such as loading a document, modifying its content,
saving it (including saving to a different output destination), and closing
it must follow a meaningful order. The validity and effect of each operation
depend on the current stage of the document. For example, a document must be
loaded before it can be modified or saved, and once it is closed, further
operations should no longer be valid.

Because the behavior of this feature depends on operation sequences and
implicit usage rules, the document lifecycle is well suited to be abstracted
as a finite state machine. This abstraction captures the allowed transitions
between stages and provides a structured representation of the feature’s behavior.

### 4.3 FSM

We model the PDF document session lifecycle in PDFBox as a finite state machine (FSM),
where each node represents an abstract document state and each directed edge represents
an API operation that causes a state transition. This model is an abstraction of
many concrete program configurations into a small set of meaningful lifecycle stages.

```mermaid
stateDiagram-v2
    state "S0 NotLoaded" as S0
    state "S1 LoadedClean" as S1
    state "S2 LoadedDirty" as S2
    state "S3 SavedAsClean" as S3
    state "S4 Closed" as S4
    S0 --> S1: load (Loader.loadPDF)
    S1 --> S2: modify (addPage)
    S2 --> S2: modify (addPage)
    S2 --> S1: save
    S2 --> S3: saveAs (different output)
    S1 --> S3: saveAs (different output)
    S3 --> S2: modify (addPage)
    S1 --> S4: close
    S2 --> S4: close
    S3 --> S4: close
```

The FSM contains five states: NotLoaded, LoadedClean, LoadedDirty, SavedAsClean,
and Closed. A successful `Loader.loadPDF(...)` transition moves the system
from NotLoaded to LoadedClean. Calling `addPage(new PDPage())` represents a
modification and transitions the document to LoadedDirty, where additional
modifications keep it in the same state.

From LoadedDirty, saving returns the document to a clean state (LoadedClean).
Saving to a different output destination (abstracted as “saveAs”) transitions
the document to SavedAsClean. From SavedAsClean, another modification
returns the document to the dirty state. The `close()` operation transitions
the document from any active state to Closed, after which further
operations are treated as invalid usage.

This FSM makes the allowed operation sequences explicit and highlights
invalid sequences (such as performing save or modify after close).
It provides a compact behavioral specification of the lifecycle and
a clear structure for systematic testing based on states and transitions.

### 4.4 Testing

run```mvn -pl pdfbox -Dtest=TestPDDocumentLifecycleFSM test```
src is
in <https://github.com/burgger/pdfbox-SWtesting/blob/trunk/pdfbox/src/test/java/org/apache/pdfbox/pdmodel/TestPDDocumentLifecycleFSM.java>

Test cases:

| Test ID                       | FSM Coverage (States/Transitions)                              | Steps                                                                        | Expected Result                                              |
|-------------------------------|----------------------------------------------------------------|------------------------------------------------------------------------------|--------------------------------------------------------------|
| T1 Load→Close                 | NotLoaded→LoadedClean, LoadedClean→Closed                      | `loadPDF(fixture)` then `close()`                                            | Load succeeds; page count = 1; close succeeds (no exception) |
| T2 Load→Modify→Save→Close     | LoadedClean→LoadedDirty, LoadedDirty→LoadedClean, →Closed      | load → `addPage` → `save(out)` → close → reload(out)                         | Reload succeeds; page count becomes 2                        |
| T3 Dirty self-loop            | LoadedDirty→LoadedDirty (modify), then LoadedDirty→LoadedClean | load → `addPage` → `addPage` → `save(out)` → reload(out)                     | Reload succeeds; page count becomes 3                        |
| T4 Clean save self-loop       | LoadedClean→LoadedClean (save)                                 | load → `save(out)` (no modify) → reload(out)                                 | Reload succeeds; page count remains 1                        |
| T5 saveAs from clean          | LoadedClean→SavedAsClean (abstract)                            | load → `save(outA)` → `save(outB)` → reload(outB)                            | Reload succeeds; page count remains 1                        |
| T6 saveAs from dirty          | LoadedDirty→SavedAsClean (abstract)                            | load → `save(outA)` → `addPage` → `save(outB)` → reload(outB)                | Reload succeeds; page count becomes 2                        |
| T7 After saveAs modify/save   | SavedAsClean→LoadedDirty, LoadedDirty→LoadedClean              | load → `save(outA)` → `save(outB)` → `addPage` → `save(outC)` → reload(outC) | Reload succeeds; page count becomes 2                        |
| T8 Invalid save after close   | Closed + `save` (invalid transition)                           | load → close → `save(out)`                                                   | Save should not succeed (exception)                          |
| T9 Invalid modify after close | Closed + `modify` (invalid transition)                         | load → close → `addPage`                                                     | not successfully produce a valid saved PDF(exception)        |

We ran 9 FSM-derived JUnit 5 tests on `two-columns.pdf` (1 page).
All tests passed. The suite covers the main lifecycle operations
load → modify (addPage) → save / saveAs → close, using
reload-and-page-count checks as the oracle. For invalid
usage after `close()`, we verified that post-close operations
cannot successfully produce a valid saved PDF. Outputs
are written to `target/test-output/fsm-test`.

## 5 Structural Testing

### 5.1 What & Why

Structural testing, also known as white-box testing, evaluates a test suite by
looking at the internal structure of the program rather than only its external
specification.
Instead of focusing only on the specification, it examines
the code itself. Functional testing is based on intended behavior,
while structural testing is based on how the program is actually implemented (Code).

Structural testing relies on the Control Flow Graph (CFG). In a CFG, nodes represent
basic blocks, and edges represent possible control flow between them. Using
this model, we can see which parts of the code are executed by our tests.

Two common coverage criteria are statement coverage and branch coverage.
Statement coverage requires that every executable statement be executed
at least once. Branch coverage requires that every branch be taken at least once.

And to measure coverage we use instrumentation. Instrumentation inserts probes into
the program and records whether statements or branches are executed.For our
PDFBox project, structural testing is practical because PDF handling code has many internal
branches. A PDF file can be valid or invalid. It can use different encode methods, filters,
and object types. PDFBox often chooses different running paths based on these cases.
Branch coverage can show which of these paths never run in our current tests.
This tells us what is missing. Then we can add the missing tests, such as tests
that trigger error-handling branches (malformed input), uncommon parsing branches,
or early-exit/exception exits in methods. This is hard to see from the API
behavior alone, but it becomes clear when we look at CFG
branches and coverage results.

### 5.2 Baseline Structural Coverage

At the project level, JaCoCo
(<https://github.com/burgger/pdfbox-SWtesting/blob/trunk/jacoco%20Baseline/index.html>)
reports 62% instruction coverage(68,767 missed out of 182,134
total instructions) and 53% branch coverage (7,615 missed out of
16,363 total branches). The project has 39,064 total lines, with 15,399
lines missed (about 61% line coverage). It also has 7,058 total methods,
with 3,051 methods missed (about 57% method coverage).

This baseline shows that many branches and methods are still not
executed by tests. In particular, recovery paths and error-handling
code are likely to be missed, because normal tests mostly use
well-formed PDFs and common workflows.

https://github.com/burgger/pdfbox-SWtesting/blob/trunk/jacoco%20Baseline/org.apache.pdfbox.pdfparser/BruteForceParser.java.html

### 5.3 Testing

#### 5.3.1 BruteForceParser

We selected `org.apache.pdfbox.pdfparser.BruteForceParser`as our testing target based on structural coverage 
analysis from the JaCoCo baseline report.
The class had 53<https://github.com/burgger/pdfbox-SWtesting/blob/trunk/jacoco%20Baseline/org.apache.pdfbox.pdfparser/BruteForceParser.html> 
uncovered lines, closely matching our requirement to
supplement at least 50 lines of new coverage. More importantly, the uncovered 
code was concentrated in a few core methods (e.g., `bfSearchForXRefStreams()`,
`bfSearchForXRef(long)`, and `compareCOSObjects(...)`) that implement recovery
logic for malformed PDFs. These branches are deterministic and can be triggered
by carefully constructed byte-level PDF inputs, making them significantly more 
controllable than rendering, font, or annotation subsystems. From a testing 
strategy perspective, this class offered the best trade-off between impact
(high uncovered density), feasibility (limited external dependencies),
and precision (clear input–path mapping).

#### 5.3.2 Added Tests

We added a new JUnit test class `org.apache.pdfbox.pdfparser.TestStructuralParser`
<https://github.com/burgger/pdfbox-SWtesting/blob/trunk/pdfbox/src/test/java/org/apache/pdfbox/pdfparser/TestStructuralParser.java>
with three targeted tests to improve structural coverage in the PDF
parsing recovery logic.

1. `testBruteForce()`

    - What it does: This test forces brute-force recovery during parsing by giving the loader a PDF whose startxref
      points to an invalid offset, so the normal xref lookup fails and PDFBox falls back to BruteForceParser. The goal
      is specifically to execute the xref-stream recovery scan logic.

    - `BruteForce.pdf` contains(in PDF structure):
        - A valid-looking set of objects (`1 0 obj` Catalog, `2 0 obj` Pages, `3 0 obj` Page, etc.)
        - A special object containing `<< /Type /XRef ... >>` (so the byte sequence "/XRef" exists in the file)
        - `startxref 123456`, which points beyond the file end, meaning the parser cannot jump to a real xref
          table/stream and must attempt recovery by scanning bytes
    - This makes `BruteForceParser` search for "`/XRef`" in the raw bytes and then scan backwards to find the nearest
      matching "` obj`" header with digits, to “fix” the xref-stream reference.
    - Cover Lines:
      L672-731<https://github.com/burgger/pdfbox-SWtesting/blob/7268eb149aa7f78116d6cf794a121f89ddbfc03b/pdfbox/src/main/java/org/apache/pdfbox/pdfparser/BruteForceParser.java#L672>


2. `test_bfSearchForXRef_chooseStreamWhenCloser_and_chooseTableWhenCloser()`

    - What it does: This test targets the decision branch in `bfSearchForXRef(long xrefOffset)` when BOTH candidates
      exist:

        - a candidate xref table offset (`xref`)

        - a candidate xref stream offset (detected via `/XRef`)

    - Then it forces the code to choose the nearer one by calling `bfSearchForXRef()` twice with different "target"
      offsets.
    - Cover Lines:
      L233-258<https://github.com/burgger/pdfbox-SWtesting/blob/7268eb149aa7f78116d6cf794a121f89ddbfc03b/pdfbox/src/main/java/org/apache/pdfbox/pdfparser/BruteForceParser.java#L233>


3. `test_compareCOSObjects_sameNumberDifferentGeneration_hitsTernaryBranch()`

    - What it does: This test uses reflection to invoke the private method `compareCOSObjects(...)` with two
      `COSObjects` that share the same object number but different generation numbers. The intent is to execute the
      ternary branch that prefers the higher generation when object numbers match.

    - Cover Lines: L498-507<https://github.com/burgger/pdfbox-SWtesting/blob/7268eb149aa7f78116d6cf794a121f89ddbfc03b/pdfbox/src/main/java/org/apache/pdfbox/pdfparser/BruteForceParser.java#L498>

### 5.4 Conclusion

| Scope                 | Metric                            | Baseline | Modified | Improvement |
|-----------------------|-----------------------------------|----------|----------|-------------|
| **BruteForceParser**  | Instruction Coverage              | 86%      | 98%      | +12%        |
| **BruteForceParser**  | Branch Coverage                   | 69%      | 82%      | +13%        |
| **BruteForceParser**  | Missed Lines                      | 53       | 8        | −45 lines   |
| **BruteForceParser**  | `bfSearchForXRef(long)` Instr.    | 49%      | 100%     | +51%        |
| **BruteForceParser**  | `bfSearchForXRefStreams()` Instr. | 14%      | 100%     | +86%        |
| **pdfparser package** | Instruction Coverage              | 82%      | 88%      | +6%         |
| **pdfparser package** | Branch Coverage                   | 69%      | 74%      | +5%         |
| **Total**             | Missed Lines                      | 15399    | 15343    | -56 lines   |


## 6 Continuous Integration

### 6.1 Definition
Integration refers to the process of combining two or more software components into
a working system. Even if individual components function correctly in isolation,
new defects may appear when they interact. Poor integration can lead to cascading
failures, making debugging difficult and time-consuming.

Continuous Integration (CI) is a development practice in which developers frequently
integrate their code changes into a shared repository, often multiple times per day.
Each integration is automatically verified by building the system and running automated tests.
The goal is to detect problems as early as possible.

The primary purposes of Continuous Integration are:

- **Early bug detection** – Smaller changes make it easier to identify and fix defects.

- **Preventing defect accumulation** – Bugs are discovered independently rather than 
interacting with one another.

- **Maintaining a stable main branch** – The main development line should always build successfully.

- **Rapid feedback** – Developers receive immediate information about whether their
changes break the system.

- **Supporting frequent deployment** – CI enables faster release cycles and continuous improvement.

In practice, CI works by automatically triggering a build and test process whenever
new code is committed to the repository. If the build or tests fail, the developer
responsible must fix the issue immediately. This ensures that the codebase remains
stable and continuously deployable.

In this project, we implement CI using GitHub Actions to automatically
build the PDFBox system and execute our test suite on every commit.

### 6.2 Existing GitHub Actions

The original PDFBox fork repository already contains a GitHub Actions workflow
named CodeQL under `.github/workflows/codeql-analysis.yml`.

This workflow is triggered on pushes and pull requests to the trunk branch.
Its primary purpose is to perform static code analysis using GitHub’s CodeQL tool.
The workflow:

- Checks out the repository

- Caches the local Maven dependencies

- Compiles the project using Maven (with tests skipped)

- Executes CodeQL security and quality analysis

CodeQL is a static analysis tool provided by GitHub that automatically scans source
code for potential security vulnerabilities and code quality issues. Unlike traditional
CI pipelines that build and run test cases, CodeQL analyzes the code structure
without executing the program.

The workflow builds the source code to enable static analysis, but it does not execute
the project's test suite, as tests are explicitly skipped during compilation (`-DskipTests)`.

### 6.3 CI Using GitHub Actions

Our task is to create a configuration file to **build** and **test** your project.

So we created a new workflow file under `.github/workflows/CI-build-test.yml`. It is triggered on:

- ```push``` to the `trunk` branch

- ```pull_request``` targeting the `trunk` branch

And run ```mvn test``` with JDK 11.

We push the yml to GitHub and wait for the GitHub Action run our workflow.

#### 6.3.1 First CI Execution

After the push, GitHub automate run our workflow start **build** and **test**. However, the test
failed on `example` module.

**Problem Encountered:** The test `TestCreateSignature.testAddValidationInformation`
failed causing the test to error.
<https://github.com/burgger/pdfbox-SWtesting/actions/runs/22396140154/job/64830154305#step:4:5875>
![First CI.png](First%20CI.png)

**Problem Analysis:** During the initial CI execution, the workflow failed in the `pdfbox-examples` module,
specifically in the test TestCreateSignature.testAddValidationInformation. The failure occurred because the
test attempts to retrieve an issuer certificate from an external AIA (Authority Information Access) URL:
<http://www.pki.admin.ch/aia/RootCAII.crt>
The GitHub Actions runner was unable to download the required issuer certificate, resulting in the error:
``` 
Error:    TestCreateSignature.testAddValidationInformation:924 » IO No Issuer Certificate found
for Cert: 'CN=Swiss Government TSA, OU=Time Stamp Services, OU=Swiss Government PKI, O=Bundesamt
fuer Informatik und Telekommunikation (BIT), OID.2.5.4.97=VATCH-CHE-221.032.573, L=Bern, C=CH',
i.e. Cert 'CN=Swiss Government Regulated CA 02, OU=Swiss Government PKI, O=Bundesamt fuer
Informatik und Telekommunikation (BIT), OID.2.5.4.97=VATCH-CHE-221.032.573, C=CH' is missing 
in the chain
```
**Problem Resolution:** To ensure CI stability and maintain a reliable build pipeline, we modified the workflow
to exclude the `pdfbox-examples` module from CI test execution. The updated Maven command in the workflow was changed to:
```yaml
mvn -B -ntp test -pl '!examples' -am
```

We push the modified yml to GitHub.

#### 6.3.2 Latest CI Execution

After applying this modification and pushing the changes to the trunk branch, the GitHub
Actions workflow executed successfully. The project built correctly, and the automated test
suite for the core modules passed without errors.
<https://github.com/burgger/pdfbox-SWtesting/actions/runs/22397638754/job/64835313041>
![Last CI.png](Last%20CI.png)

This adjustment ensures that the CI pipeline remains stable, fast, and deterministic,
while still providing automated verification of the main project components.


## 7 Testable Design

A testable design is one that makes behavior easy to verify in a stable, repeatable way by letting
tests control dependencies and inputs and observe outputs clearly. The slides emphasize that
when code depends on external components (e.g., DB/network/memory) that may be unimplemented,
unreliable, or hard to control in tests, we should replace those dependencies with test doubles
to isolate the unit under test. In that framing, a stub returns hard-coded or simplified results,
a mock focuses on verifying how a dependency was used (calls/arguments), and a spy records interactions
with a real object for later inspection.

To improve testable:
- Avoid hard-coding object:creation with new inside methods (it prevents stubbing/mocking; prefer creating the object
outside and injecting it). 
- Avoid complex private methods because private code cannot be directly
tested.
- Be cautious with static methods—especially those with side effects or randomness—because
they are difficult or impossible to stub. Also keep logic out of constructors
(constructors are hard to bypass; move logic into overridable methods), and avoid rigid patterns
like Singletons that are hard to replace in tests.

### 7.1 Bad Testable Design in PDFBox

We chose the `idTime` logic inside `COSWriter.write()` as our bad testable design
example because it hard-codes an uncontrollable dependency—`System.currentTimeMillis()`—directly
into a core output-producing path. When `pdDocument.getDocumentId() == null`, the writer
uses the current system time as an input to the SHA-256 computation that generates the trailer `/ID` values.
This makes the output inherently non-deterministic: the same document content can produce different
IDs across runs, which makes unit tests difficult to write with stable assertions and increases the
risk of flaky tests. This aligns with the slide guidance that static calls
(especially those involving side effects or uncontrollable behavior) are difficult to stub
and therefore reduce testability.

### 7.2 Fix it Testable and Test

We will extract a protected method as a time seam(<https://github.com/burgger/pdfbox-SWtesting/blob/deed0f983a29df785ef89205c107e03e38b7de11/pdfbox/src/main/java/org/apache/pdfbox/pdfwriter/COSWriter.java#L1532>).

Specifically, we will add a method in `COSWriter`:

- ```protected long nowMillis() { return System.currentTimeMillis(); }```

and replace the direct static call with:

- `long idTime = (pdDocument.getDocumentId() == null) ? nowMillis() : pdDocument.getDocumentId();`

This keeps production behavior unchanged (default still uses the system clock), but in
tests(<https://github.com/burgger/pdfbox-SWtesting/blob/deed0f983a29df785ef89205c107e03e38b7de11/pdfbox/src/test/java/org/apache/pdfbox/pdfwriter/COSWriterTest.java#L197>) we can create a small `TestCOSWriter extends COSWriter` that overrides `nowMillis()`
to return a fixed value, making `/ID` generation deterministic and easy to assert.

## 8 Mocking

Mocking is a testing technique that replaces a real dependency with a fake object designed
for interaction verification. Instead of focusing on the dependency’s real behavior or outputs,
a mock allows a test to assert how the dependency is used—for example, whether a method was called,
how many times it was called, and with which arguments. This is especially valuable when the dependency
is expensive to set up, non-deterministic, or external to the unit under test.

In our project, mocking helps us write focused unit tests that validate a component’s
collaboration with its dependencies without relying on real implementations.
We will use Mockito to create and control mock objects and to verify the expected interactions, 
keeping tests fast, deterministic, and isolated.

### 8.1 Mocking in PDFBox

We selected a mocking target from code adjacent to our previous "bad testable design"
location in `COSWriter.write()`
(<https://github.com/burgger/pdfbox-SWtesting/blob/deed0f983a29df785ef89205c107e03e38b7de11/pdfbox/src/main/java/org/apache/pdfbox/pdfwriter/COSWriter.java#L1557>). 
In the same method, the writer collaborates with the encryption subsystem by obtaining a `SecurityHandler`
from the document's encryption configuration and invoking `prepareDocumentForEncryption(PDDocument)`.
This is an interaction-oriented behavior: what matters for a focused unit test is not the internal
encryption implementation, but whether the writer triggers the encryption preparation step under
the correct conditions. Therefore, it is a natural fit for mocking, where we verify how a dependency is used.

Our plan uses a simple form of dependency injection to enable mocking: instead of relying
on a concrete `SecurityHandler` implementation, we supply (or intercept) the handler as a replaceable
dependency in the test, and use Mockito to create a mock handler. With this setup, the test
can deterministically assert that `prepareDocumentForEncryption(doc)` is invoked (or not invoked)
without executing real encryption logic, keeping the test isolated and fast. Mockito provides
the mechanism to create and manage the mock and to verify the expected interactions.

### 8.2 Test
We implemented a Mockito-based unit test to validate the encryption interaction in `COSWriter.write()`
(<https://github.com/burgger/pdfbox-SWtesting/blob/trunk/pdfbox/src/test/java/org/apache/pdfbox/pdfwriter/COSWriterMockingTest.java>).
Instead of executing real encryption setup, the test injects a mocked encryption dependency and a mocked
`SecurityHandler`, then runs `COSWriter.write(doc, null)`. The core assertion is interaction-based: we verify 
that `prepareDocumentForEncryption(PDDocument)` is invoked exactly once when encryption is present and the
writer is performing a non-incremental write. This matches the purpose of mocking in the slides—using a
fake object to check how a dependency is used (calls and arguments), rather than validating the dependency's
real behavior.

To enable this, we apply a simple form of dependency injection at the test level by replacing the document's
encryption configuration with a Mockito mock, which returns our mocked `SecurityHandler`. This keeps the test
fast, deterministic, and isolated from the complexity of actual encryption logic. Mockito is used to create 
and manage the mock objects and to perform the interaction verification.

## 9 Static Analysis

Static analysis refers to examining source code without executing the program. 
The goal of static analysis is to detect potential defects and code quality issues 
early in the development process. Because the analysis is performed directly on 
the code, it can identify suspicious patterns such as possible null pointer usage, 
resource leaks, or violations of coding practices before the software is run.

Code review is also considered a form of static analysis since it involves 
manually inspecting code without executing it. Automated static analyzers extend 
this idea by automatically scanning the codebase and reporting potential problems. 
However, these tools are considered pessimistic analyses because they may report 
warnings that are not actual bugs, so developers must interpret the results and 
determine whether the reported issues represent real problems.

### 9.1 CodeQL in PDFBox

#### 9.1.1 Enable CodeQL

The repository already contained a CodeQL workflow, but it used an outdated
version of the GitHub CodeQL Action
<https://github.com/burgger/pdfbox-SWtesting/blob/341f5e7aa524ee57f2c0995b5f8b48d140ec665b/.github/workflows/codeql-analysis.yml>.
To avoid maintaining a deprecated configuration, we switched the repository to GitHub’s default CodeQL
setup and disable the original one, the default CodeQL setup automatically generates
and manages a supported CodeQL configuration.

After enabling GitHub's CodeQL analysis using the default setup, the repository
produced **11 open alerts**. Most of the findings were categorized as
**high severity**, primarily related to cryptographic practices and
potential numeric conversion issues. A smaller number of alerts were
related to workflow configuration.

#### 9.1.2 Warning in CodeQL
One example warning reported by CodeQL is **"Implicit narrowing conversion in compound assignment"**,
detected in `PDLineDashPattern.java`<https://github.com/burgger/pdfbox-SWtesting/security/code-scanning/9>. This warning indicates that a compound
assignment may implicitly perform a narrowing type conversion, which could
potentially lead to loss of precision or overflow.

In Java, compound assignments such as `+=` may automatically cast the
result back to the original variable type. For example, if a smaller
numeric type is used, the intermediate computation may exceed the
representable range of the variable. CodeQL flags this situation
because it may introduce subtle bugs in numerical computations.

However, in the context of the PDFBox implementation, this conversion appears 
to be intentional and controlled by the developers. Therefore, although the
warning highlights a potentially risky pattern, it does not represent a real 
defect in this particular case.

### 9.2 SpotBugs in PDFBox

#### 9.2.1 Run SpotBugs

As an additional static analysis tool, we applied SpotBugs to the project. 

```bash
mvn -pl pdfbox com.github.spotbugs:spotbugs-maven-plugin:spotbugs com.github.spotbugs:spotbugs-maven-plugin:gui
```
SpotBugs analyzes compiled Java bytecode and detects common bug patterns such as 
null pointer dereferences, bad programming practices, concurrency issues, and 
potential security vulnerabilities.

Running SpotBugs on the pdfbox module produced 692 reported issues. 
These issues were categorized into several groups, including malicious code 
vulnerability patterns, bad practices, multithreaded correctness problems, 
and dodgy code patterns.

A large portion of the findings (553 issues) were classified as malicious code 
vulnerabilities. However, many of these warnings are likely false positives, 
since the tool often flags generic patterns that may not represent real security 
problems in the context of a mature library such as Apache PDFBox.

#### 9.2.2 Warning in SpotBugs

One example warning reported by SpotBugs is **“Possible bad parsing of shift operation”** 
(Bug pattern: `BSHIFT_WRONG_ADD_PRIORITY`). This warning appears in 
`SampledImageReader.java` at line 428.

The warning indicates that a shift operation combined with an arithmetic expression 
may be parsed differently than intended due to Java operator precedence rules. 
In Java, the shift operator (`<<`) has lower precedence than addition, which means 
an expression such as `x << 8 + y` is interpreted as `x << (8 + y)` rather than 
`(x << 8) + y`.
![SpotBugs.png](SpotBugs.png)

SpotBugs flags this pattern because developers sometimes intend to perform the 
shift before the addition, but the expression may be interpreted differently 
by the compiler if parentheses are not used clearly.

In the PDFBox code, the expression

`(buff[r] ^ invert) << (24 + (x & 7))`

already explicitly uses parentheses to control evaluation order. Because the 
addition is intentionally grouped inside parentheses, the shift operation is 
performed after the arithmetic calculation as intended. Therefore, this warning 
likely represents a conservative warning from the analyzer rather than an actual 
bug in the implementation.

### 9.3 Comparison of Static Analysis Tools

In this project I used two static analysis tools: CodeQL and SpotBugs. 
Both tools analyze the code without running the program, but they focus on 
different kinds of issues.

CodeQL mainly reported security and correctness related warnings. 
With the default configuration it only produced a small number of alerts 
in the repository. These warnings usually point to patterns that may cause 
security risks or unsafe behavior in the code.

SpotBugs reported many more issues. This tool analyzes Java bytecode and 
checks for common bug patterns such as bad practices, potential logic errors, 
or concurrency problems. Because it contains a much larger set of rules, 
it often produces a large number of warnings.

From this experiment it is clear that different static analysis tools provide 
different perspectives on the code. CodeQL focuses on higher-confidence 
security problems, while SpotBugs reports a wider range of potential coding 
issues. Using both tools together gives a better overall view of possible 
problems in the project.


